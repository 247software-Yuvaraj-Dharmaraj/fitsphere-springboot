package com.yuvaraj.fitsphere.service;

import com.yuvaraj.fitsphere.domain.Slot;
import com.yuvaraj.fitsphere.dto.SlotDtos.BulkDeleteResult;
import com.yuvaraj.fitsphere.dto.SlotDtos.CreateSlotRequest;
import com.yuvaraj.fitsphere.dto.SlotDtos.ListByDate;
import com.yuvaraj.fitsphere.dto.SlotDtos.SlotDto;
import com.yuvaraj.fitsphere.dto.SlotDtos.UpdateSlotRequest;
import com.yuvaraj.fitsphere.exception.HttpException;
import com.yuvaraj.fitsphere.realtime.RealtimeService;
import com.yuvaraj.fitsphere.repository.SlotRepository;
import com.yuvaraj.fitsphere.util.TimeUtil;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SlotService {

    private final SlotRepository slots;
    private final RealtimeService realtime;

    public SlotService(SlotRepository slots, RealtimeService realtime) {
        this.slots = slots;
        this.realtime = realtime;
    }

    // ---- reads ----

    public ListByDate listByDate(String userId, String date) {
        String target = date != null ? date : LocalDate.now(ZoneOffset.UTC).toString();
        Instant from = dayStart(target);
        Instant to = from.plus(1, ChronoUnit.DAYS);
        List<SlotDto> dtos = slots.findInDay(from, to).stream()
                .map(s -> toDto(s, userId))
                .toList();
        return new ListByDate(target, dtos);
    }

    public List<SlotDto> myBookings(String userId) {
        Instant today = TimeUtil.startOfDay();
        return slots.findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(today).stream()
                .filter(s -> s.getBookings().contains(userId) || s.getWaitlist().contains(userId))
                .map(s -> toDto(s, userId))
                .toList();
    }

    // ---- member actions ----

    public SlotDto book(String slotId, String userId) {
        Slot slot = require(slotId);
        if (slot.getDate().isBefore(TimeUtil.startOfDay())) {
            throw new HttpException(HttpStatus.CONFLICT, "This slot has already passed");
        }
        if (slot.getBookings().contains(userId)) {
            throw new HttpException(HttpStatus.CONFLICT, "You have already booked this slot");
        }
        if (slot.getBookings().size() >= slot.getCapacity()) {
            throw new HttpException(HttpStatus.CONFLICT, "This slot is fully booked");
        }
        slot.getWaitlist().remove(userId);
        slot.getBookings().add(userId);
        slots.save(slot);
        realtime.emitSlotsChanged();
        return toDto(slot, userId);
    }

    public SlotDto cancel(String slotId, String userId) {
        Slot slot = require(slotId);
        boolean wasBooked = slot.getBookings().remove(userId);
        boolean wasWaitlisted = slot.getWaitlist().remove(userId);
        if (!wasBooked && !wasWaitlisted) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "You have not booked this slot");
        }
        if (wasBooked) {
            promoteFromWaitlist(slot);
        }
        slots.save(slot);
        realtime.emitSlotsChanged();
        return toDto(slot, userId);
    }

    public SlotDto joinWaitlist(String slotId, String userId) {
        Slot slot = require(slotId);
        if (slot.getDate().isBefore(TimeUtil.startOfDay())) {
            throw new HttpException(HttpStatus.CONFLICT, "This slot has already passed");
        }
        if (slot.getBookings().contains(userId)) {
            throw new HttpException(HttpStatus.CONFLICT, "You have already booked this slot");
        }
        if (slot.getBookings().size() < slot.getCapacity()) {
            throw new HttpException(HttpStatus.CONFLICT, "This slot still has space — book it directly");
        }
        if (slot.getWaitlist().contains(userId)) {
            throw new HttpException(HttpStatus.CONFLICT, "You are already on the waitlist");
        }
        slot.getWaitlist().add(userId);
        slots.save(slot);
        realtime.emitSlotsChanged();
        return toDto(slot, userId);
    }

    public SlotDto leaveWaitlist(String slotId, String userId) {
        Slot slot = require(slotId);
        if (!slot.getWaitlist().remove(userId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "You are not on the waitlist");
        }
        slots.save(slot);
        realtime.emitSlotsChanged();
        return toDto(slot, userId);
    }

    // ---- staff actions ----

    public SlotDto create(CreateSlotRequest in) {
        if (in.startTime().compareTo(in.endTime()) >= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        Instant from = dayStart(in.date());
        if (from.isBefore(TimeUtil.startOfDay())) {
            throw new HttpException(HttpStatus.CONFLICT, "Cannot create a slot in the past");
        }
        assertNoOverlap(from, from.plus(1, ChronoUnit.DAYS), in.startTime(), in.endTime(), null);
        Slot slot = new Slot();
        slot.setDate(from);
        slot.setStartTime(in.startTime());
        slot.setEndTime(in.endTime());
        slot.setCapacity(in.capacity());
        slots.save(slot);
        return toDto(slot, "");
    }

    public SlotDto update(String slotId, UpdateSlotRequest in) {
        if (in.startTime().compareTo(in.endTime()) >= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        Slot slot = require(slotId);
        if (in.capacity() < slot.getBookings().size()) {
            throw new HttpException(HttpStatus.CONFLICT,
                    "Capacity can't be below the " + slot.getBookings().size() + " existing booking(s)");
        }
        Instant dayFrom = TimeUtil.startOfDay(slot.getDate());
        assertNoOverlap(dayFrom, dayFrom.plus(1, ChronoUnit.DAYS), in.startTime(), in.endTime(), slot.getId());
        slot.setStartTime(in.startTime());
        slot.setEndTime(in.endTime());
        slot.setCapacity(in.capacity());
        promoteFromWaitlist(slot);
        slots.save(slot);
        realtime.emitSlotsChanged();
        return toDto(slot, "");
    }

    public void remove(String slotId) {
        Slot slot = require(slotId);
        slots.delete(slot);
    }

    public BulkDeleteResult bulkRemove(List<String> ids) {
        List<String> valid = ids.stream().filter(ObjectId::isValid).toList();
        List<Slot> found = slots.findAllById(valid);
        slots.deleteAll(found);
        return new BulkDeleteResult(found.size());
    }

    // ---- helpers ----

    private Slot require(String slotId) {
        if (!ObjectId.isValid(slotId)) {
            throw new HttpException(HttpStatus.NOT_FOUND, "Slot not found");
        }
        return slots.findById(slotId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Slot not found"));
    }

    private void promoteFromWaitlist(Slot slot) {
        while (slot.getBookings().size() < slot.getCapacity() && !slot.getWaitlist().isEmpty()) {
            slot.getBookings().add(slot.getWaitlist().remove(0));
        }
    }

    private void assertNoOverlap(Instant from, Instant to, String startTime, String endTime, String excludeId) {
        List<Slot> sameDay = slots.findInDay(from, to);
        boolean clash = sameDay.stream()
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .anyMatch(s -> startTime.compareTo(s.getEndTime()) < 0 && s.getStartTime().compareTo(endTime) < 0);
        if (clash) {
            throw new HttpException(HttpStatus.CONFLICT, "This slot overlaps an existing slot on that day");
        }
    }

    private static Instant dayStart(String dateYmd) {
        return LocalDate.parse(dateYmd).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private SlotDto toDto(Slot s, String userId) {
        int bookedCount = s.getBookings().size();
        int waitlistIndex = s.getWaitlist().indexOf(userId);
        return new SlotDto(
                s.getId(),
                s.getDate(),
                s.getStartTime(),
                s.getEndTime(),
                s.getCapacity(),
                bookedCount,
                Math.max(0, s.getCapacity() - bookedCount),
                s.getBookings().contains(userId),
                bookedCount >= s.getCapacity(),
                s.getWaitlist().size(),
                waitlistIndex != -1,
                waitlistIndex == -1 ? null : waitlistIndex + 1);
    }
}

package com.yuvaraj.fitsphere.web;

import com.yuvaraj.fitsphere.dto.SlotDtos.BulkDeleteRequest;
import com.yuvaraj.fitsphere.dto.SlotDtos.BulkDeleteResult;
import com.yuvaraj.fitsphere.dto.SlotDtos.CreateSlotRequest;
import com.yuvaraj.fitsphere.dto.SlotDtos.ListByDate;
import com.yuvaraj.fitsphere.dto.SlotDtos.SlotDto;
import com.yuvaraj.fitsphere.dto.SlotDtos.UpdateSlotRequest;
import com.yuvaraj.fitsphere.security.AppUser;
import com.yuvaraj.fitsphere.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotService slots;

    public SlotController(SlotService slots) {
        this.slots = slots;
    }

    @GetMapping
    public ListByDate list(@RequestParam(required = false) String date, @AuthenticationPrincipal AppUser principal) {
        return slots.listByDate(principal.id(), date);
    }

    @GetMapping("/my-bookings")
    public List<SlotDto> myBookings(@AuthenticationPrincipal AppUser principal) {
        return slots.myBookings(principal.id());
    }

    @PostMapping("/{id}/book")
    public SlotDto book(@PathVariable String id, @AuthenticationPrincipal AppUser principal) {
        return slots.book(id, principal.id());
    }

    @DeleteMapping("/{id}/book")
    public SlotDto cancel(@PathVariable String id, @AuthenticationPrincipal AppUser principal) {
        return slots.cancel(id, principal.id());
    }

    @PostMapping("/{id}/waitlist")
    public SlotDto joinWaitlist(@PathVariable String id, @AuthenticationPrincipal AppUser principal) {
        return slots.joinWaitlist(id, principal.id());
    }

    @DeleteMapping("/{id}/waitlist")
    public SlotDto leaveWaitlist(@PathVariable String id, @AuthenticationPrincipal AppUser principal) {
        return slots.leaveWaitlist(id, principal.id());
    }

    @PostMapping
    public ResponseEntity<SlotDto> create(@Valid @RequestBody CreateSlotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slots.create(req));
    }

    @PostMapping("/bulk-delete")
    public BulkDeleteResult bulkDelete(@Valid @RequestBody BulkDeleteRequest req) {
        return slots.bulkRemove(req.ids());
    }

    @PatchMapping("/{id}")
    public SlotDto update(@PathVariable String id, @Valid @RequestBody UpdateSlotRequest req) {
        return slots.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable String id) {
        slots.remove(id);
        return ResponseEntity.noContent().build();
    }
}

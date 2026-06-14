package com.yuvaraj.fitsphere.config;

import com.yuvaraj.fitsphere.domain.Attendance;
import com.yuvaraj.fitsphere.domain.Feedback;
import com.yuvaraj.fitsphere.domain.GymConfig;
import com.yuvaraj.fitsphere.domain.Role;
import com.yuvaraj.fitsphere.domain.Slot;
import com.yuvaraj.fitsphere.domain.User;
import com.yuvaraj.fitsphere.domain.WorkoutLog;
import com.yuvaraj.fitsphere.domain.WorkoutType;
import com.yuvaraj.fitsphere.repository.AttendanceRepository;
import com.yuvaraj.fitsphere.repository.FeedbackRepository;
import com.yuvaraj.fitsphere.repository.GymConfigRepository;
import com.yuvaraj.fitsphere.repository.SlotRepository;
import com.yuvaraj.fitsphere.repository.UserRepository;
import com.yuvaraj.fitsphere.repository.WorkoutLogRepository;
import com.yuvaraj.fitsphere.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * TEMP — rich demo dataset for recruiter-facing demos.
 *
 * Seeds a full member roster with backdated attendance history, the demo member's
 * streak/workouts/bookings/feedback, a week of slots with realistic bookings, and enough
 * cross-member activity to populate the admin analytics. When {@code app.seed.reset} is true
 * (the default while demoing) it wipes and reseeds on every boot so the live demo self-heals;
 * otherwise it only seeds an empty database. Revert this commit to restore the minimal seed.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";
    private static final int GYM_CAPACITY = 50;

    /** Roster (besides the demo member Alice) so analytics and the member directory look real. */
    private static final String[][] ROSTER = {
            {"Marcus Lee", "marcus.lee@fitsphere.app"},
            {"Priya Nair", "priya.nair@fitsphere.app"},
            {"Diego Santos", "diego.santos@fitsphere.app"},
            {"Sara Kim", "sara.kim@fitsphere.app"},
            {"Tom Becker", "tom.becker@fitsphere.app"},
            {"Lena Park", "lena.park@fitsphere.app"},
            {"Omar Farah", "omar.farah@fitsphere.app"},
            {"Nina Patel", "nina.patel@fitsphere.app"},
            {"Jack Ryan", "jack.ryan@fitsphere.app"},
            {"Mei Chen", "mei.chen@fitsphere.app"},
            {"Ravi Kumar", "ravi.kumar@fitsphere.app"},
    };

    private final UserRepository users;
    private final GymConfigRepository gymConfig;
    private final SlotRepository slots;
    private final AttendanceRepository attendance;
    private final WorkoutLogRepository workouts;
    private final FeedbackRepository feedback;
    private final PasswordEncoder encoder;
    private final boolean reset;

    public DataSeeder(UserRepository users, GymConfigRepository gymConfig, SlotRepository slots,
                      AttendanceRepository attendance, WorkoutLogRepository workouts,
                      FeedbackRepository feedback, PasswordEncoder encoder,
                      @Value("${app.seed.reset:false}") boolean reset) {
        this.users = users;
        this.gymConfig = gymConfig;
        this.slots = slots;
        this.attendance = attendance;
        this.workouts = workouts;
        this.feedback = feedback;
        this.encoder = encoder;
        this.reset = reset;
    }

    @Override
    public void run(String... args) {
        // Never let a seeding problem stop the app from starting.
        try {
            seed();
        } catch (Exception e) {
            log.error("Demo seeding failed (app still starts): {}", e.getMessage(), e);
        }
    }

    private void seed() {
        if (reset) {
            log.info("SEED_RESET=true — clearing existing data before reseeding...");
            feedback.deleteAll();
            workouts.deleteAll();
            attendance.deleteAll();
            slots.deleteAll();
            gymConfig.deleteAll();
            users.deleteAll();
        } else if (users.count() > 0) {
            return;
        }
        log.info("Seeding demo data...");

        Random rnd = new Random(42);
        Instant base = TimeUtil.startOfDay();

        // ---- accounts ----
        User member = createUser("Alice Member", "member@fitsphere.app", Role.MEMBER);
        User trainer = createUser("Tina Trainer", "trainer@fitsphere.app", Role.TRAINER);
        createUser("Adam Admin", "admin@fitsphere.app", Role.ADMIN);

        List<User> members = new ArrayList<>();
        members.add(member);
        for (String[] r : ROSTER) {
            members.add(createUser(r[0], r[1], Role.MEMBER));
        }

        GymConfig config = new GymConfig();
        config.setCapacity(GYM_CAPACITY);
        gymConfig.save(config);

        // ---- demo member: a strong current streak + a deep history ----
        // 11 consecutive days ending today (hits the 3- and 7-day milestones); today stays open.
        for (int d = 10; d >= 0; d--) {
            int hour = (d % 2 == 0) ? 7 : 18;
            Instant in = base.minus(d, ChronoUnit.DAYS).plus(hour, ChronoUnit.HOURS);
            Attendance a = new Attendance(member.getId(), in);
            if (d > 0) {
                a.setCheckOutAt(in.plus(rnd.nextInt(60) + 45, ChronoUnit.MINUTES));
            }
            attendance.save(a);
        }
        // Older visits earlier in the month so weekly/monthly totals look real.
        for (int d : new int[]{13, 15, 17, 19, 22, 24, 27}) {
            int hour = (d % 2 == 0) ? 8 : 19;
            Instant in = base.minus(d, ChronoUnit.DAYS).plus(hour, ChronoUnit.HOURS);
            Attendance a = new Attendance(member.getId(), in);
            a.setCheckOutAt(in.plus(rnd.nextInt(60) + 45, ChronoUnit.MINUTES));
            attendance.save(a);
        }

        // ---- demo member: ~24 workouts across types over the last month ----
        WorkoutType[] types = WorkoutType.values();
        for (int i = 0; i < 24; i++) {
            WorkoutType type = types[i % types.length];
            int duration = 30 + (i % 5) * 10; // 30..70
            Instant when = base.minus(i + 1L, ChronoUnit.DAYS).plus(7 + (i % 12), ChronoUnit.HOURS);
            workouts.save(new WorkoutLog(member.getId(), type, duration, when));
        }

        // ---- the rest of the roster: varied attendance for analytics + member directory ----
        for (int m = 1; m < members.size(); m++) {
            User u = members.get(m);
            boolean inactive = (m % 7 == 0); // a few stale members to show status variety
            int visits = inactive ? 2 + rnd.nextInt(3) : 8 + rnd.nextInt(14);
            int oldestDay = inactive ? 40 : 26;
            for (int v = 0; v < visits; v++) {
                int daysAgo = inactive ? 18 + rnd.nextInt(oldestDay - 18) : rnd.nextInt(oldestDay);
                int hour = peakHour(rnd);
                Instant in = base.minus(daysAgo, ChronoUnit.DAYS).plus(hour, ChronoUnit.HOURS);
                Attendance a = new Attendance(u.getId(), in);
                a.setCheckOutAt(in.plus(rnd.nextInt(70) + 40, ChronoUnit.MINUTES));
                attendance.save(a);
            }
        }

        // A handful currently checked-in (open) today so live occupancy is non-trivial.
        for (int m = 2; m <= 7; m++) {
            User u = members.get(m);
            Instant in = base.plus(rnd.nextInt(3) + 8, ChronoUnit.HOURS);
            attendance.save(new Attendance(u.getId(), in));
        }

        // ---- slots: a full week ahead with realistic bookings + a waitlist ----
        seedSlots(base, members, member, rnd);

        // ---- feedback: trainer notes to the demo member + a few others ----
        Instant week = TimeUtil.startOfWeek();
        saveFeedback(trainer, member, "Great consistency this week — keep the cardio sessions going!", week);
        saveFeedback(trainer, member, "Nice progress on strength. Add one mobility session next week.", week.minus(7, ChronoUnit.DAYS));
        saveFeedback(trainer, member, "Strong streak! Watch your recovery on back-to-back days.", week.minus(14, ChronoUnit.DAYS));
        saveFeedback(trainer, members.get(1), "Welcome back — let's rebuild the routine gradually.", week);
        saveFeedback(trainer, members.get(2), "Excellent form in the strength circuit today.", week);
        saveFeedback(trainer, members.get(3), "Try the 7am slot — it fits your schedule better.", week.minus(7, ChronoUnit.DAYS));

        log.info("Seed complete: {} users, {} attendance, {} workouts, {} slots, {} feedback.",
                users.count(), attendance.count(), workouts.count(), slots.count(), feedback.count());
        log.info("Demo accounts (password {}): {}", DEMO_PASSWORD,
                List.of("member@fitsphere.app", "trainer@fitsphere.app", "admin@fitsphere.app"));
    }

    private void seedSlots(Instant base, List<User> members, User member, Random rnd) {
        // {startHour, endHour, capacity}
        int[][] times = {{6, 7, 15}, {7, 8, 20}, {12, 13, 12}, {17, 18, 20}, {18, 19, 25}, {19, 20, 20}};
        List<String> memberIds = members.stream().map(User::getId).toList();
        for (int d = 0; d < 7; d++) {
            Instant day = base.plus(d, ChronoUnit.DAYS);
            for (int[] t : times) {
                Slot s = new Slot();
                s.setDate(day);
                s.setStartTime(String.format("%02d:00", t[0]));
                s.setEndTime(String.format("%02d:00", t[1]));
                s.setCapacity(t[2]);
                // Clamp to the roster size — can't book more distinct members than exist.
                int fill = Math.min(memberIds.size(), Math.min(t[2], rnd.nextInt(t[2] / 2 + 1) + t[2] / 3));
                List<String> shuffled = new ArrayList<>(memberIds);
                Collections.shuffle(shuffled, rnd);
                for (int i = 0; i < fill; i++) {
                    s.getBookings().add(shuffled.get(i));
                }
                slots.save(s);
            }
        }

        // Book the demo member into two upcoming slots so "My Bookings" isn't empty.
        bookMemberInto(member, base.plus(1, ChronoUnit.DAYS), "07:00", "08:00", 20);
        bookMemberInto(member, base.plus(2, ChronoUnit.DAYS), "18:00", "19:00", 25);

        // A popular, fully-booked slot tomorrow with the demo member on the waitlist.
        Slot full = new Slot();
        full.setDate(base.plus(1, ChronoUnit.DAYS));
        full.setStartTime("12:00");
        full.setEndTime("13:00");
        full.setCapacity(2);
        List<String> others = new ArrayList<>(members.stream().map(User::getId).filter(id -> !id.equals(member.getId())).toList());
        Collections.shuffle(others, rnd);
        full.getBookings().add(others.get(0));
        full.getBookings().add(others.get(1));
        full.getWaitlist().add(member.getId());
        slots.save(full);
    }

    private void bookMemberInto(User member, Instant day, String start, String end, int capacity) {
        Slot s = new Slot();
        s.setDate(day);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setCapacity(capacity);
        s.getBookings().add(member.getId());
        slots.save(s);
    }

    private static int peakHour(Random rnd) {
        // Weighted toward morning (6-9) and evening (17-20) peaks.
        int[] hours = {6, 7, 7, 8, 8, 9, 12, 17, 18, 18, 19, 19, 20};
        return hours[rnd.nextInt(hours.length)];
    }

    private void saveFeedback(User trainer, User member, String note, Instant weekOf) {
        Feedback fb = new Feedback();
        fb.setTrainer(trainer.getId());
        fb.setMember(member.getId());
        fb.setNote(note);
        fb.setWeekOf(weekOf);
        feedback.save(fb);
    }

    private User createUser(String name, String email, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        u.setRole(role);
        return users.save(u);
    }
}

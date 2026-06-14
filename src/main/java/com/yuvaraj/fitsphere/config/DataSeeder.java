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
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Seeds demo accounts, gym config, slots, attendance, workouts and feedback on first run. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository users;
    private final GymConfigRepository gymConfig;
    private final SlotRepository slots;
    private final AttendanceRepository attendance;
    private final WorkoutLogRepository workouts;
    private final FeedbackRepository feedback;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, GymConfigRepository gymConfig, SlotRepository slots,
                      AttendanceRepository attendance, WorkoutLogRepository workouts,
                      FeedbackRepository feedback, PasswordEncoder encoder) {
        this.users = users;
        this.gymConfig = gymConfig;
        this.slots = slots;
        this.attendance = attendance;
        this.workouts = workouts;
        this.feedback = feedback;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        log.info("Seeding demo data...");

        User member = createUser("Alice Member", "member@fitsphere.app", Role.MEMBER);
        User trainer = createUser("Tina Trainer", "trainer@fitsphere.app", Role.TRAINER);
        createUser("Adam Admin", "admin@fitsphere.app", Role.ADMIN);

        GymConfig config = new GymConfig();
        config.setCapacity(50);
        gymConfig.save(config);

        // Slots across the next 3 days.
        int[][] times = {{6, 7, 15}, {7, 8, 15}, {12, 13, 10}, {18, 19, 20}, {19, 20, 20}};
        Instant base = TimeUtil.startOfDay();
        for (int d = 0; d < 3; d++) {
            Instant day = base.plus(d, ChronoUnit.DAYS);
            for (int[] t : times) {
                Slot s = new Slot();
                s.setDate(day);
                s.setStartTime(String.format("%02d:00", t[0]));
                s.setEndTime(String.format("%02d:00", t[1]));
                s.setCapacity(t[2]);
                slots.save(s);
            }
        }

        // Member attendance over recent days (some consecutive → a streak), all closed.
        int[] daysAgo = {1, 2, 3, 5, 6, 8, 9, 11};
        for (int i = 0; i < daysAgo.length; i++) {
            int hour = 6 + (i % 5) * 3; // vary hours for the peak-time analytics
            Instant in = base.minus(daysAgo[i], ChronoUnit.DAYS).plus(hour, ChronoUnit.HOURS);
            Attendance a = new Attendance(member.getId(), in);
            a.setCheckOutAt(in.plus(1, ChronoUnit.HOURS));
            attendance.save(a);
        }

        // A few workouts for the member.
        WorkoutType[] types = {WorkoutType.CARDIO, WorkoutType.STRENGTH, WorkoutType.MIXED, WorkoutType.CARDIO, WorkoutType.STRENGTH};
        for (int i = 0; i < types.length; i++) {
            workouts.save(new WorkoutLog(member.getId(), types[i], 30 + i * 10,
                    base.minus(i + 1L, ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS)));
        }

        // One demo feedback note.
        Feedback fb = new Feedback();
        fb.setTrainer(trainer.getId());
        fb.setMember(member.getId());
        fb.setNote("Great consistency this week — keep the cardio sessions going!");
        fb.setWeekOf(TimeUtil.startOfWeek());
        feedback.save(fb);

        log.info("Seed complete. Demo accounts (password {}): {}", DEMO_PASSWORD,
                List.of("member@fitsphere.app", "trainer@fitsphere.app", "admin@fitsphere.app"));
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

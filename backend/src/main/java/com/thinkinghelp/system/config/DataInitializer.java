package com.thinkinghelp.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize Admin User compliant with new rules (Pure numbers, 7-12 digits)
        // Account: 1000000
        // Password: admin123
        String adminAccount = "1000000";

        if (!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, adminAccount))) {
            User admin = new User();
            admin.setUsername(adminAccount);
            admin.setNickname("Administrator");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setGender(1);
            admin.setRole("ADMIN");
            admin.setMemberLevel(1); // VIP

            userMapper.insert(admin);
            System.out.println("Admin user initialized: " + adminAccount);
        }
    }
}

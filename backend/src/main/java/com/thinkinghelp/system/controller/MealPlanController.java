package com.thinkinghelp.system.controller;

import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.service.ExportService;
import com.thinkinghelp.system.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Tag(name = "食谱生成", description = "AI 食谱生成与导出")
public class MealPlanController {

    private final ExportService exportService;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @GetMapping("/api/meal-plan/weekly")
    @Operation(summary = "生成一周食谱")
    public Result<MealPlanDTO> generateWeekly(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        MealPlanDTO plan = exportService.generateWeeklyMealPlan(userId);
        return Result.success(plan);
    }

    @GetMapping("/api/export/pdf")
    @Operation(summary = "导出食谱 PDF")
    public void exportPdf(@RequestHeader("Authorization") String token, HttpServletResponse response)
            throws IOException {
        Long userId = getUserIdFromToken(token);
        MealPlanDTO plan = exportService.generateWeeklyMealPlan(userId);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"meal_plan.pdf\"");
        exportService.exportMealPlanToPdf(plan, response.getOutputStream());
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = jwtUtils.extractUsername(token);
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        return user.getId();
    }
}

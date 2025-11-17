package com.sheep.sphunter;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.sheep.sphunter.analyse.HunterAnalysis;
import com.sheep.sphunter.analyse.NativeHunterAnalysis;
import com.sheep.sphunter.databinding.ActivityMainBinding;
import com.sheep.sphunter.fingerprint.FingerprintService;
import com.sheep.sphunter.model.FingerprintResult;

import org.json.JSONObject;

/**
 * 主界面 Activity
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FingerprintService fingerprintService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化指纹采集服务
        fingerprintService = new FingerprintService(this);

        setupClickListeners();
    }

    /**
     * 设置按钮点击监听器
     */
    private void setupClickListeners() {
        // Java 层指纹采集按钮
        binding.button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectJavaFingerprint();
            }
        });

        // Native 层指纹采集按钮
        binding.button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectNativeFingerprint();
            }
        });
    }

    /**
     * 采集 Java 层指纹（原始数据）
     */
    private void collectJavaFingerprint() {
        try {
            binding.textViewResult.setText("正在采集 Java 层指纹...\n");
            
            // 1. 采集原始数据
            FingerprintResult result = fingerprintService.collectJavaFingerprint();
            
            // 2. 清洗数据
            JSONObject cleanedData = fingerprintService.collectAndCleanJavaFingerprint();
            
            // 3. 显示结果
            StringBuilder output = new StringBuilder();

            HunterAnalysis.Result resultFromAnalysis = HunterAnalysis.analyze(fingerprintService.getCleanedFingerprintString());

            output.append("══════════════════════\n");
            output.append("📱 数据评估\n");
            output.append("══════════════════════\n\n");
            output.append("Stable Device ID: " + resultFromAnalysis.deviceId.substring(0,16)+"...");
            output.append("\nRisk Analysis: " + resultFromAnalysis.riskReport);
            output.append("\nEmulator Status: "+ resultFromAnalysis.isEmulator);

            output.append("\n\r══════════════════════\n");
            // output.append("✨ 清洗后的指纹数据（结构化）\n");
            output.append("📱 Java 层指纹信息\n");
            output.append("══════════════════════\n\n");
            output.append(fingerprintService.getCleanedFingerprintString());
        //    output.append(result.toString());
            output.append("\n\n");

            binding.textViewResult.setText(output.toString());
        } catch (Exception e) {
            String errorMsg = "❌ 错误: " + e.getMessage();
            binding.textViewResult.setText(errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * 采集 Native 层指纹
     */
    private void collectNativeFingerprint() {
        try {
            binding.textViewResult.setText("正在采集 Native 层指纹...\n");
            
            // 1. 采集原始数据
            FingerprintResult result = fingerprintService.collectNativeFingerprint();
            
            // 2. 清洗数据
            JSONObject cleanedNativeData = fingerprintService.getCleanedNativeFingerprint();
            
            // 3. 使用 NativeHunterAnalysis 分析数据
            NativeHunterAnalysis.Result analysisResult = NativeHunterAnalysis.analyze(cleanedNativeData.toString());
            
            StringBuilder output = new StringBuilder();
            
            // 显示分析结果
            output.append("══════════════════════\n");
            output.append("🛡️ Native 层数据评估\n");
            output.append("══════════════════════\n\n");
            
            // 设备唯一标识
            output.append("🔑 Native Device ID: ");
            if (analysisResult.nativeDeviceId != null && analysisResult.nativeDeviceId.length() > 16) {
                output.append(analysisResult.nativeDeviceId.substring(0, 16)).append("...");
            } else {
                output.append(analysisResult.nativeDeviceId);
            }
            output.append("\n\n");
            
            // 风险评分
            output.append("📊 风险评分: ").append(analysisResult.riskScore).append("/100");
            if (analysisResult.riskScore >= 70) {
                output.append(" 🔴 高危");
            } else if (analysisResult.riskScore >= 40) {
                output.append(" 🟡 中危");
            } else if (analysisResult.riskScore > 0) {
                output.append(" 🟢 低危");
            } else {
                output.append(" ✅ 安全");
            }
            output.append("\n\n");
            
            // 各项检测结果
            output.append("🔍 检测结果:\n");
            output.append("  • 模拟器: ").append(analysisResult.isEmulator ? "❌ 是" : "✅ 否").append("\n");
            output.append("  • Root/解锁: ").append(analysisResult.isRooted ? "❌ 是" : "✅ 否").append("\n");
            output.append("  • 调试模式: ").append(analysisResult.isDebugMode ? "⚠️ 是" : "✅ 否").append("\n");
            output.append("  • Zygisk注入: ").append(analysisResult.hasZygiskInjection ? "❌ 是" : "✅ 否").append("\n");
            output.append("\n");
            
            // 风险报告
            output.append("📋 风险报告:\n");
            output.append(analysisResult.riskReport);
            output.append("\n\n");
            
            // 显示清洗后的结构化数据（可折叠查看）
            output.append("══════════════════════\n");
            output.append("⚙️ Native 层指纹信息\n");
            output.append("══════════════════════\n\n");
            
            if (cleanedNativeData.length() > 0) {
                output.append("✨ 清洗后的指纹数据（结构化）\n");
                output.append("══════════════════════\n\n");
                output.append(cleanedNativeData.toString(2)); // 格式化 JSON，缩进 2 个空格
                output.append("\n\n");
            }
            
            // 显示原始数据（可选，用于调试）
            // output.append("📄 原始数据（调试用）\n");
            // output.append("══════════════════════\n\n");
            // output.append(result.toNativeString());
            
            // 获取 MAC 地址（可选）
            // String macAddress = fingerprintService.getMacAddress();
            // if (macAddress != null && !macAddress.isEmpty()) {
            //     output.append("\n\n📡 MAC 地址: ").append(macAddress);
            // }
            
            binding.textViewResult.setText(output.toString());
        } catch (Exception e) {
            String errorMsg = "❌ 错误: " + e.getMessage();
            binding.textViewResult.setText(errorMsg);
            e.printStackTrace();
        }
    }
}

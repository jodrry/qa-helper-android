package com.example.qahelper.network

import com.example.qahelper.data.TYPE_PHOTO

object MockAnswer {

    fun build(question: String, type: String, hasImage: Boolean): String {
        val cleanQuestion = question.trim().replace(Regex("\\s+"), " ")
        val summary = when {
            cleanQuestion.isNotBlank() -> cleanQuestion.take(120)
            hasImage && type == TYPE_PHOTO -> "图片中的题目"
            else -> "未填写题干"
        }

        val imageNote = if (type == TYPE_PHOTO && hasImage) {
            "已收到图片，但模拟模式不会调用真实多模态接口，仅演示完整交互流程。"
        } else {
            "当前为本地模拟回答，不会消耗接口额度。"
        }

        return buildString {
            appendLine("【模拟模式】")
            appendLine(imageNote)
            appendLine()
            appendLine("一、题目摘要")
            appendLine(summary)
            appendLine()
            appendLine("二、分步骤解答")
            appendLine("1. 梳理已知条件：先圈出题目中的已知量、未知量以及限制条件，确认题目到底要求什么。")
            appendLine("2. 选择解题方法：根据题型匹配对应知识点，例如方程、函数、几何定理或文本分析方法。")
            appendLine("3. 分步推导：把复杂问题拆成小步骤，每一步只做一个变换，并写清楚依据。")
            appendLine("4. 检查结果：回代或反向验证，检查单位、范围和边界情况，排除明显不合理的答案。")
            appendLine()
            appendLine("三、最终答案")
            appendLine("由于当前处于模拟模式，以上内容用于演示界面、历史记录与交互流程，并不是针对此题的真实推理结果。")
            appendLine("在「设置」中关闭模拟模式并填入 API Key 后，即可获得真实模型给出的完整解答。")
        }.trim()
    }
}

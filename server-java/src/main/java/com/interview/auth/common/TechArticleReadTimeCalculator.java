package com.interview.auth.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * 统一按技术文章正文长度估算阅读时长，避免历史库值长期停留在固定分钟数。
 * 先把 HTML 转成纯文本并统计中英文阅读单元，再按统一速率换算成最少 1 分钟的展示文案。
 */
public final class TechArticleReadTimeCalculator {

    private static final int MIN_READ_MINUTES = 1;
    private static final int READABLE_UNITS_PER_MINUTE = 420;
    private static final int CODE_BLOCK_READ_UNITS = 120;
    private static final int LIST_ITEM_READ_UNITS = 12;
    private static final int MEDIA_READ_UNITS = 90;
    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9]+(?:[._'/-][A-Za-z0-9]+)*");
    private static final Pattern MINUTE_PATTERN = Pattern.compile("(\\d+)");

    private TechArticleReadTimeCalculator() {
    }

    /**
     * 根据正文内容返回统一的阅读时长文案，兼容数据库里已有的旧字段值。
     * 正文为空时优先回退到原字段里的分钟数，仍然没有可用值时再返回 1 min 保证前端稳定展示。
     */
    public static String estimateReadTime(String contentHtml, String fallbackReadTime) {
        Document document = parseDocument(contentHtml);
        int readableUnits = countReadableUnits(document.text()) + countStructureUnits(document);
        if (readableUnits > 0) {
            int minutes = Math.max(MIN_READ_MINUTES, (int) Math.round(readableUnits / (double) READABLE_UNITS_PER_MINUTE));
            return minutes + " min";
        }
        int fallbackMinutes = extractMinutes(fallbackReadTime);
        return Math.max(fallbackMinutes, MIN_READ_MINUTES) + " min";
    }

    /**
     * 把文章 HTML 解析成可重复统计的文档对象，避免多个入口各自处理标签细节。
     * Jsoup 会统一修正文档片段结构，后续文字和结构权重都从同一个节点树读取。
     */
    private static Document parseDocument(String contentHtml) {
        return Jsoup.parseBodyFragment(defaultString(contentHtml));
    }

    /**
     * 同时统计中文字符和英文单词数量，尽量贴近技术文章里中英混排的真实阅读节奏。
     * 中文按单字累计，英文和数字按连续词组累计，最后合并成统一的阅读单元总数。
     */
    private static int countReadableUnits(String plainText) {
        if (plainText.isBlank()) {
            return 0;
        }
        int chineseCharacterCount = 0;
        for (int index = 0; index < plainText.length(); index++) {
            if (Character.UnicodeScript.of(plainText.charAt(index)) == Character.UnicodeScript.HAN) {
                chineseCharacterCount++;
            }
        }
        int latinTokenCount = 0;
        Matcher matcher = LATIN_TOKEN_PATTERN.matcher(plainText);
        while (matcher.find()) {
            latinTokenCount++;
        }
        return chineseCharacterCount + latinTokenCount;
    }

    /**
     * 统计代码块、列表和媒体元素带来的额外阅读成本，让技术文章估算更贴近真实停留时间。
     * 按节点数量增加稳定权重，避免只有纯文字长度时多篇结构相近文章被压成同一分钟数。
     */
    private static int countStructureUnits(Document document) {
        int codeBlockCount = document.select("pre, code").size();
        int listItemCount = document.select("li").size();
        int mediaCount = document.select("img, table").size();
        return codeBlockCount * CODE_BLOCK_READ_UNITS
            + listItemCount * LIST_ITEM_READ_UNITS
            + mediaCount * MEDIA_READ_UNITS;
    }

    /**
     * 从旧的分钟数字段里提取可复用的数值，兼容 6 min 和 6 分钟这类历史格式。
     * 解析失败时返回 0，让上层继续走统一的最小分钟数兜底而不是沿用脏数据。
     */
    private static int extractMinutes(String fallbackReadTime) {
        Matcher matcher = MINUTE_PATTERN.matcher(defaultString(fallbackReadTime));
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    /**
     * 把可能出现的空值统一收口成空字符串，减少静态工具在不同入口下的判空分支。
     * 这样 Service 和后台保存链路都可以直接复用同一套时长计算逻辑。
     */
    private static String defaultString(String value) {
        return value == null ? "" : value;
    }
}

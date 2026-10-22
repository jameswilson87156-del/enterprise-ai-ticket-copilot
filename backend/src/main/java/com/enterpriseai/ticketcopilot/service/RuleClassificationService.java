package com.enterpriseai.ticketcopilot.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class RuleClassificationService {

    private static final List<Pattern> EXPLICIT_INJECTION_INSTRUCTIONS = List.of(
        injectionPattern("\\bignore\\s+(?:all\\s+)?(?:prior|previous)\\s+instructions?\\b"),
        injectionPattern("\\bignore\\s+(?:the\\s+)?(?:rules|policy)\\b"),
        injectionPattern("\\b(?:print|reveal|show|output|share)\\b.{0,48}\\b(?:hidden\\s+)?system\\s+prompt\\b"),
        injectionPattern("\\b(?:reveal|show|print|output|expose)\\b.{0,48}\\b(?:api\\s+key|credentials?|secret|token)\\b"),
        injectionPattern("\\b(?:label|classify)\\s+(?:this|the\\s+ticket|the\\s+case)\\s+(?:as|to)\\b"),
        injectionPattern("\\b(?:i am|i'm)\\b.{0,60}\\b(?:admin|administrator)\\b.{0,60}\\b(?:bypass|override|label|classify)\\b"),
        injectionPattern("\\b(?:run|execute|perform)\\b[^.!?。！？\\r\\n]{0,80}\\b(?:drop\\s+table|delete\\s+from|truncate|rm\\s+-rf)\\b"),
        injectionPattern("(?:忽略|无视).{0,12}(?:所有|之前|先前|前面).{0,12}(?:指令|规则|提示)"),
        injectionPattern("(?:输出|打印|展示|泄露|返回).{0,16}(?:系统提示词|系统提示|api\\s*密钥|密钥|凭证)"),
        injectionPattern("(?:把|将).{0,12}(?:分类|类别|工单).{0,12}(?:改成|设置为|归为|分类为)"),
        injectionPattern("(?:执行|运行)[^.!?。！？\\r\\n]{0,32}(?:drop\\s+table|delete\\s+from|truncate|rm\\s+-rf)")
    );
    private static final Pattern PERCENT_ENCODED_ASCII = Pattern.compile("%([0-7][0-9a-fA-F])");
    private static final Pattern NUMERIC_HTML_ENTITY = Pattern.compile("&#(?:x([0-9a-fA-F]{1,6})|([0-9]{1,7}));");
    private static final Pattern FRAMED_BASE64 = Pattern.compile(
        "(?i)\\b(BASE64URL|B64URL|BASE64|B64):([A-Za-z0-9_+/-]{8,4096}={0,2})"
    );
    private static final Pattern FRAMED_HEX = Pattern.compile("(?i)\\bHEX:([0-9A-F]{8,4096})\\b");
    private static final Pattern FRAMED_ROT13 = Pattern.compile("(?i)\\bROT13:([A-Za-z][A-Za-z0-9_,.!?'’ \\-]{7,256})");

    private static final String SENTENCE_BOUNDARIES = ".!?。！？\r\n";
    private static final Pattern LABELED_TICKET_ISSUE = Pattern.compile(
        "^(?:actual\\s+(?:issue|problem)\\s*[:：]|实际问题\\s*[:：])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static Pattern injectionPattern(String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static final List<String> SYSTEM_INCIDENT_SIGNALS = List.of(
        "超时", "timeout", "502", "500", "connection reset", "连接重置", "连接被重置", "丢包", "heartbeat", "心跳",
        "stale", "health check", "健康检查", "连接池", "connection pool", "服务无响应", "服务响应中断",
        "任务停止", "停止推进", "队列停滞", "调度心跳", "待处理队列"
    );

    private static final List<RuleBucket> RULES = List.of(
        new RuleBucket("账号问题", List.of("账号", "登录", "登陆", "密码", "mfa", "sso", "锁定", "无法登录", "认证")),
        new RuleBucket("权限问题", List.of("权限", "授权", "访问", "审批", "角色", "共享盘", "rbac", "permission", "forbidden")),
        new RuleBucket("系统故障", List.of(
            "报错", "故障", "502", "500", "timeout", "超时", "异常", "不可用", "宕机", "失败", "error",
            "白屏", "connection reset", "连接重置", "连接被重置", "丢包", "heartbeat", "心跳", "stale",
            "health check", "健康检查", "连接池", "connection pool", "exhausted", "服务无响应", "请求持续排队", "服务响应中断",
            "任务停止", "停止推进", "队列停滞", "调度心跳", "待处理队列"
        )),
        new RuleBucket("数据问题", List.of(
            "数据", "同步", "sync", "缺失", "重复", "duplicate", "不一致", "报表", "指标", "导入", "导出", "聚合", "对不上", "字段映射", "映射", "错位"
        )),
        new RuleBucket("流程咨询", List.of("流程", "如何", "怎么", "咨询", "申请", "规范", "步骤", "指引", "顺序"))
    );

    public RuleClassificationResult classify(String title, String description, String errorLog) {
        String text = normalize(stripExplicitInjectionSentences(title) + " "
            + stripExplicitInjectionSentences(description) + " " + stripExplicitInjectionSentences(errorLog));
        String bestCategory = "流程咨询";
        int bestScore = 0;
        for (RuleBucket rule : RULES) {
            int score = 0;
            for (String keyword : rule.keywords()) {
                if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                    score++;
                }
            }
            if ("权限问题".equals(rule.category()) && hasPeerAccessContrast(text)) {
                score += 2;
            }
            if ("系统故障".equals(rule.category()) && containsAny(text, SYSTEM_INCIDENT_SIGNALS)) {
                score++;
            }
            if (score > bestScore) {
                bestScore = score;
                bestCategory = rule.category();
            }
        }
        int confidence = Math.min(92, 58 + bestScore * 11);
        return new RuleClassificationResult(bestCategory, confidence);
    }

    private boolean hasPeerAccessContrast(String text) {
        boolean mentionsPeer = containsAny(text, List.of("同事", "同组", "同岗位", "其他成员", "其他人", "同一部门", "旁边工位"));
        boolean reportsAccessProblem = containsAny(text, List.of(
            "打不开", "看不到", "没有对应入口", "没有这个入口", "没有导出按钮", "页面没有", "不显示", "不可见", "没有这个操作入口"
        ));
        return mentionsPeer && reportsAccessProblem;
    }

    private boolean containsAny(String text, List<String> phrases) {
        return phrases.stream().anyMatch(text::contains);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    /**
     * Removes only sentence spans containing a recognizable explicit override,
     * extraction, role-spoofing, or destructive-action instruction. Keeping later
     * sentences preserves a real ticket symptom after a prefixed attack. This narrow
     * heuristic is a defense-in-depth measure, not a general injection detector.
     */
    String stripExplicitInjectionSentences(String field) {
        if (field == null || field.isBlank()) {
            return "";
        }

        String sanitized = normalizeUntrustedText(field);
        String decoded = decodeAsciiTextEncodings(sanitized);
        if (!decoded.equals(sanitized) && containsExplicitInjection(decoded)) {
            sanitized = decoded;
        }
        while (true) {
            Matcher earliestInstruction = null;
            for (Pattern pattern : EXPLICIT_INJECTION_INSTRUCTIONS) {
                Matcher matcher = pattern.matcher(sanitized);
                if (matcher.find() && (earliestInstruction == null || matcher.start() < earliestInstruction.start())) {
                    earliestInstruction = matcher;
                }
            }

            if (earliestInstruction == null) {
                return sanitized;
            }

            int sentenceStart = earliestInstruction.start();
            while (sentenceStart > 0 && !isSentenceBoundary(sanitized.charAt(sentenceStart - 1))) {
                sentenceStart--;
            }
            int sentenceEnd = earliestInstruction.end();
            while (sentenceEnd < sanitized.length() && !isSentenceBoundary(sanitized.charAt(sentenceEnd))) {
                sentenceEnd++;
            }
            int labeledIssueBoundary = findLabeledIssueBoundary(
                sanitized, earliestInstruction.end(), sentenceEnd
            );
            if (labeledIssueBoundary >= 0) {
                sentenceEnd = labeledIssueBoundary + 1;
            } else if (sentenceEnd < sanitized.length()) {
                sentenceEnd++;
            }
            sanitized = sanitized.substring(0, sentenceStart) + sanitized.substring(sentenceEnd);
        }
    }

    private int findLabeledIssueBoundary(String text, int searchStart, int sentenceEnd) {
        for (int index = searchStart; index < sentenceEnd; index++) {
            char character = text.charAt(index);
            if (character != ';' && character != '；') {
                continue;
            }
            String clause = text.substring(index + 1, sentenceEnd).stripLeading();
            if (LABELED_TICKET_ISSUE.matcher(clause).find() && !containsExplicitInjection(clause)) {
                return index;
            }
        }
        return -1;
    }

    private boolean containsExplicitInjection(String text) {
        return EXPLICIT_INJECTION_INSTRUCTIONS.stream().anyMatch(pattern -> pattern.matcher(text).find());
    }

    String normalizeUntrustedText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        StringBuilder sanitized = new StringBuilder(normalized.length());
        normalized.codePoints()
            .filter(codePoint -> Character.getType(codePoint) != Character.FORMAT)
            .forEach(sanitized::appendCodePoint);
        return sanitized.toString();
    }

    boolean containsExplicitInjectionInstruction(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = normalizeUntrustedText(text);
        return containsExplicitInjection(normalized)
            || containsExplicitInjection(decodeAsciiTextEncodings(normalized));
    }

    /** Decodes a bounded number of ASCII percent escapes and numeric HTML entities for policy checks. */
    String decodeAsciiTextEncodings(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String decoded = text;
        for (int pass = 0; pass < 2; pass++) {
            String next = decodePercentEncodedAscii(decoded);
            next = decodeNumericHtmlEntities(next);
            next = decodeFramedTextEncodings(next);
            if (next.equals(decoded)) {
                break;
            }
            decoded = next;
        }
        return decoded;
    }

    private String decodeFramedTextEncodings(String text) {
        String decoded = replaceDecodedMatches(text, FRAMED_BASE64, this::decodeBase64Text);
        decoded = replaceDecodedMatches(decoded, FRAMED_HEX, this::decodeHexText);
        return replaceDecodedMatches(decoded, FRAMED_ROT13, this::decodeRot13Text);
    }

    private String decodeBase64Text(Matcher matcher) {
        String token = matcher.group(2);
        String unpadded = token.replaceAll("=+$", "");
        if (unpadded.length() % 4 == 1) {
            return matcher.group();
        }
        String padded = unpadded + "=".repeat((4 - unpadded.length() % 4) % 4);
        try {
            Base64.Decoder decoder = matcher.group(1).toUpperCase(Locale.ROOT).contains("URL")
                ? Base64.getUrlDecoder() : Base64.getDecoder();
            return decodeUtf8Text(decoder.decode(padded), matcher.group());
        } catch (IllegalArgumentException exception) {
            return matcher.group();
        }
    }

    private String decodeHexText(Matcher matcher) {
        String token = matcher.group(1);
        if (token.length() % 2 != 0 || token.length() > 4096) {
            return matcher.group();
        }
        byte[] bytes = new byte[token.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            int high = Character.digit(token.charAt(index * 2), 16);
            int low = Character.digit(token.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return matcher.group();
            }
            bytes[index] = (byte) ((high << 4) + low);
        }
        return decodeUtf8Text(bytes, matcher.group());
    }

    private String decodeUtf8Text(byte[] bytes, String fallback) {
        if (bytes.length > 2048) {
            return fallback;
        }
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
            if (decoded.isEmpty() || decoded.codePoints().anyMatch(codePoint ->
                Character.isISOControl(codePoint) && codePoint != '\n' && codePoint != '\r' && codePoint != '\t')) {
                return fallback;
            }
            return decoded;
        } catch (CharacterCodingException exception) {
            return fallback;
        }
    }

    private String decodeRot13Text(Matcher matcher) {
        StringBuilder decoded = new StringBuilder(matcher.group(1).length());
        matcher.group(1).chars().forEach(character -> {
            if (character >= 'a' && character <= 'z') {
                decoded.append((char) ('a' + (character - 'a' + 13) % 26));
            } else if (character >= 'A' && character <= 'Z') {
                decoded.append((char) ('A' + (character - 'A' + 13) % 26));
            } else {
                decoded.append((char) character);
            }
        });
        return decoded.toString();
    }

    private String replaceDecodedMatches(String value, Pattern pattern, Function<Matcher, String> decoder) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(decoder.apply(matcher)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String decodePercentEncodedAscii(String text) {
        Matcher matcher = PERCENT_ENCODED_ASCII.matcher(text);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) {
            char value = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    private String decodeNumericHtmlEntities(String text) {
        Matcher matcher = NUMERIC_HTML_ENTITY.matcher(text);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) {
            String digits = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int radix = matcher.group(1) != null ? 16 : 10;
            try {
                int codePoint = Integer.parseInt(digits, radix);
                if (Character.isValidCodePoint(codePoint)
                    && (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE)) {
                    matcher.appendReplacement(decoded, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
                    continue;
                }
            } catch (IllegalArgumentException ignored) {
                // Keep malformed or out-of-range entities unchanged for the policy matcher.
            }
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    private boolean isSentenceBoundary(char character) {
        return SENTENCE_BOUNDARIES.indexOf(character) >= 0;
    }

    private record RuleBucket(String category, List<String> keywords) {
    }
}

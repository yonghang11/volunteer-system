package com.campus.volunteer.common;

/**
 * 全站字符串常量（用常量类代替枚举，简单直观）。
 */
public class Const {

    /** 角色 */
    public static class Role {
        public static final String STUDENT = "STUDENT";
        public static final String MENTOR = "MENTOR";
        public static final String LAB_ADMIN = "LAB_ADMIN";
        public static final String COLLEGE_ADMIN = "COLLEGE_ADMIN";
    }

    /** 8 个环节编码 */
    public static class Stage {
        public static final String BRIEFING = "BRIEFING";
        public static final String OPEN_DAY = "OPEN_DAY";
        public static final String INTENT = "INTENT";
        public static final String INTERVIEW = "INTERVIEW";
        public static final String CONFIRM = "CONFIRM";
        public static final String SUPPLEMENT = "SUPPLEMENT";
        public static final String TRANSFER = "TRANSFER";
        public static final String PUBLICITY = "PUBLICITY";
    }

    /** 环节状态：开放 / 未开始 / 已结束 */
    public static class Phase {
        public static final String OPEN = "OPEN";
        public static final String PENDING = "PENDING";
        public static final String CLOSED = "CLOSED";
    }

    /** 意向状态 */
    public static class IntentStatus {
        public static final String SUBMITTED = "SUBMITTED";
        public static final String OFFERED = "OFFERED";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String RELEASED = "RELEASED";
    }

    /** 拟录取结果类型 */
    public static class OfferType {
        public static final String OFFER = "OFFER";
        public static final String WAITLIST = "WAITLIST";
        public static final String INNER = "INNER";
    }

    /** 拟录取记录状态 */
    public static class OfferStatus {
        public static final String PENDING = "PENDING";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String DECLINED = "DECLINED";
        public static final String EXPIRED = "EXPIRED";
        public static final String FILLED = "FILLED";
    }

    /** 面试结论 */
    public static class Result {
        public static final String OFFER = "OFFER";
        public static final String WAITLIST = "WAITLIST";
        public static final String INNER = "INNER";
        public static final String REJECT = "REJECT";
    }

    /** 面试状态 */
    public static class InterviewStatus {
        public static final String PENDING = "PENDING";
        public static final String DONE = "DONE";
        public static final String ABSENT = "ABSENT";
    }

    /** 调剂任务状态 */
    public static class TaskStatus {
        public static final String PENDING = "PENDING";
        public static final String ACCEPTED = "ACCEPTED";
        public static final String REFUSED = "REFUSED";
        public static final String ASSIGNED = "ASSIGNED";
    }

    /** 公示状态 */
    public static class PublicStatus {
        public static final String PUBLISHED = "PUBLISHED";
        public static final String OBJECTED = "OBJECTED";
        public static final String JOINED = "JOINED";
    }
}

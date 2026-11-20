-- 学生实训室选组选拔系统 建库脚本（MySQL 8.0）
DROP DATABASE IF EXISTS zytbxt;
CREATE DATABASE zytbxt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE zytbxt;

-- 用户（学生/导师/实训室管理员/学院管理员）
CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL COMMENT 'SHA-256 摘要',
  real_name VARCHAR(64) NOT NULL,
  role VARCHAR(20) NOT NULL COMMENT 'STUDENT/MENTOR/LAB_ADMIN/COLLEGE_ADMIN',
  student_no VARCHAR(64) NULL,
  class_no VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,
  lab_id BIGINT NULL,
  placed TINYINT NOT NULL DEFAULT 0 COMMENT '是否已正式入组',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '系统用户';

-- 实训室
CREATE TABLE lab (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  direction VARCHAR(255) NULL,
  intro TEXT NULL,
  quota_total INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '实训室';

-- 导师（导师即意向填报单位）
CREATE TABLE mentor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  lab_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  name VARCHAR(64) NOT NULL,
  title VARCHAR(64) NULL,
  direction VARCHAR(255) NULL,
  quota INT NOT NULL DEFAULT 0 COMMENT '计划名额',
  intro TEXT NULL,
  accept_cross_transfer TINYINT NOT NULL DEFAULT 1,
  KEY idx_mentor_lab (lab_id)
) COMMENT '导师';

-- 8 个环节的时间窗（学院后台可改，驱动全站开关）
CREATE TABLE stage_window (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  stage_code VARCHAR(32) NOT NULL UNIQUE,
  stage_no INT NOT NULL,
  name VARCHAR(64) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  remark VARCHAR(255) NULL
) COMMENT '环节时间窗';

-- 宣讲 / 开放日排期
CREATE TABLE session_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(16) NOT NULL COMMENT 'BRIEFING/OPEN_DAY',
  lab_id BIGINT NULL,
  title VARCHAR(128) NOT NULL,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  place VARCHAR(128) NULL,
  capacity INT DEFAULT 0,
  content TEXT NULL
) COMMENT '宣讲与开放日';

-- 意向（平行，最多 3 个，不分先后）
CREATE TABLE intent (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  mentor_id BIGINT NOT NULL,
  lab_id BIGINT NOT NULL,
  round_no INT NOT NULL DEFAULT 1,
  accept_inner TINYINT NOT NULL DEFAULT 0 COMMENT '服从实训室内部调剂',
  accept_cross TINYINT NOT NULL DEFAULT 0 COMMENT '服从全院跨实训室调剂',
  status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/OFFERED/CONFIRMED/RELEASED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_stu_mentor_round (student_id, mentor_id, round_no),
  KEY idx_intent_lab (lab_id, round_no)
) COMMENT '学生意向';

-- 面试场次
CREATE TABLE interview (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  lab_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  mentor_id BIGINT NULL,
  round_no INT NOT NULL DEFAULT 1,
  slot_start DATETIME NULL,
  place VARCHAR(128) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/ABSENT',
  UNIQUE KEY uk_interview (student_id, round_no)
) COMMENT '面试安排';

-- 面试评分（学习态度/逻辑思维/编程基础/时间投入）
CREATE TABLE score (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  interview_id BIGINT NOT NULL UNIQUE,
  student_id BIGINT NOT NULL,
  mentor_id BIGINT NOT NULL,
  lab_id BIGINT NOT NULL,
  round_no INT NOT NULL DEFAULT 1,
  attitude INT NULL,
  logic INT NULL,
  coding INT NULL,
  commitment INT NULL,
  total INT NULL,
  result VARCHAR(20) NULL COMMENT 'OFFER/WAITLIST/INNER/REJECT',
  remark VARCHAR(255) NULL,
  scorer BIGINT NULL,
  score_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '面试评分';

-- 拟录取 / 递补 / 内部调剂候选
CREATE TABLE offer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  mentor_id BIGINT NOT NULL,
  lab_id BIGINT NOT NULL,
  round_no INT NOT NULL DEFAULT 1,
  type VARCHAR(16) NOT NULL COMMENT 'OFFER/WAITLIST/INNER',
  rank_no INT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/DECLINED/EXPIRED/FILLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirm_time DATETIME NULL,
  UNIQUE KEY uk_offer (student_id, mentor_id, round_no, type),
  KEY idx_offer_stu (student_id, status),
  KEY idx_offer_mentor (mentor_id, type, status)
) COMMENT '拟录取结果';

-- 分轮补录轮次（第 2、3 轮）
CREATE TABLE supplement_round (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  round_no INT NOT NULL UNIQUE,
  open_flag TINYINT NOT NULL DEFAULT 0,
  slots_json TEXT NULL COMMENT '本轮剩余名额快照',
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '补录轮次';

-- 统筹调剂
CREATE TABLE transfer_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  priority INT NOT NULL COMMENT '1内部 2跨室 3约谈安置',
  from_lab_id BIGINT NULL,
  to_lab_id BIGINT NULL,
  to_mentor_id BIGINT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REFUSED/ASSIGNED',
  operator VARCHAR(64) NULL,
  talk_note VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_transfer_stu (student_id)
) COMMENT '统筹调剂记录';

-- 公示名单
CREATE TABLE public_list (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  lab_id BIGINT NOT NULL,
  mentor_id BIGINT NULL,
  published_at DATETIME NULL,
  object_until DATETIME NULL,
  objection VARCHAR(255) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/OBJECTED/JOINED',
  KEY idx_public_stu (student_id)
) COMMENT '公示名单';

-- 正式入组记录
CREATE TABLE join_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  lab_id BIGINT NOT NULL,
  mentor_id BIGINT NULL,
  join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_join_stu (student_id)
) COMMENT '入组记录';

-- 站内通知
CREATE TABLE notice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(512) NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notice_user (user_id, read_flag)
) COMMENT '站内通知';

-- 操作留痕
CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor VARCHAR(64) NULL,
  action VARCHAR(64) NOT NULL,
  target VARCHAR(128) NULL,
  detail VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_time (create_time)
) COMMENT '审计日志';

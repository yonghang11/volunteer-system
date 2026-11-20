-- 种子数据
USE zytbxt;

SET @pwd = '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92'; -- 明文 123456

INSERT INTO lab (id, code, name, direction, intro, quota_total) VALUES
(1, 'LAB-A', '人工智能视觉实训室', '计算机视觉 / 图像算法', '面向 AI+PBL 项目制培养，主攻视觉检测与模型部署。', 8),
(2, 'LAB-B', '智能硬件与机器人实训室', '嵌入式 / 机器人控制', '软硬结合方向，参与机器人整机调试与嵌入式开发。', 8),
(3, 'LAB-C', '数据智能与平台开发实训室', '大数据 / 后端平台', '数据平台、微服务与工程化能力训练。', 9),
(4, 'LAB-D', '大模型应用与 AIGC 实训室', 'LLM 应用 / Agent', '围绕大模型应用、RAG 与智能体开发。', 9);

INSERT INTO sys_user (id, username, password, real_name, role, lab_id) VALUES
(1, 'admin', @pwd, '学院管理员', 'COLLEGE_ADMIN', NULL),
(2, 'labadmin1', @pwd, 'A室管理员', 'LAB_ADMIN', 1),
(3, 'labadmin2', @pwd, 'B室管理员', 'LAB_ADMIN', 2),
(4, 'labadmin3', @pwd, 'C室管理员', 'LAB_ADMIN', 3),
(5, 'labadmin4', @pwd, 'D室管理员', 'LAB_ADMIN', 4),
(6, 'm01', @pwd, '张明（A室）', 'MENTOR', 1),
(7, 'm02', @pwd, '李慧（A室）', 'MENTOR', 1),
(8, 'm03', @pwd, '王磊（A室）', 'MENTOR', 1),
(9, 'm04', @pwd, '赵敏（B室）', 'MENTOR', 2),
(10, 'm05', @pwd, '陈昊（B室）', 'MENTOR', 2),
(11, 'm06', @pwd, '周颖（B室）', 'MENTOR', 2),
(12, 'm07', @pwd, '吴涛（C室）', 'MENTOR', 3),
(13, 'm08', @pwd, '郑璇（C室）', 'MENTOR', 3),
(14, 'm09', @pwd, '孙鹏（C室）', 'MENTOR', 3),
(15, 'm10', @pwd, '钱宁（D室）', 'MENTOR', 4),
(16, 'm11', @pwd, '冯宇（D室）', 'MENTOR', 4),
(17, 'm12', @pwd, '韩雪（D室）', 'MENTOR', 4);

INSERT INTO mentor (id, lab_id, user_id, name, title, direction, quota, intro, accept_cross_transfer) VALUES
(1, 1, 6, '张明', '副教授', '工业缺陷检测', 3, '视觉检测算法与边缘部署，节奏紧凑。', 1),
(2, 1, 7, '李慧', '讲师', '图像分割', 2, '医学图像分割项目，偏科研。', 1),
(3, 1, 8, '王磊', '工程师', '模型部署', 3, '模型压缩与推理加速，偏工程。', 1),
(4, 2, 9, '赵敏', '副教授', '机器人控制', 3, '移动机器人导航与调试。', 1),
(5, 2, 10, '陈昊', '讲师', '嵌入式开发', 2, '嵌入式底层与传感器接入。', 1),
(6, 2, 11, '周颖', '工程师', '运动规划', 3, '机械臂抓取与运动规划。', 1),
(7, 3, 12, '吴涛', '副教授', '微服务架构', 4, '高并发平台与中间件。', 1),
(8, 3, 13, '郑璇', '讲师', '数据平台', 3, '数据采集、清洗与调度。', 1),
(9, 3, 14, '孙鹏', '工程师', '前端工程化', 2, '可视化与前端架构。', 1),
(10, 4, 15, '钱宁', '教授', '大模型应用', 4, 'RAG 与领域知识库建设。', 1),
(11, 4, 16, '冯宇', '讲师', '智能体开发', 3, 'Agent 编排与工具调用。', 1),
(12, 4, 17, '韩雪', '工程师', 'AIGC 工程', 2, '生成式内容与多模态工程。', 1);

-- 30 名学生（stu01 ~ stu30）
INSERT INTO sys_user (id, username, password, real_name, role, student_no, class_no, phone)
WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 30)
SELECT 17 + n,
       CONCAT('stu', LPAD(n, 2, '0')),
       @pwd,
       CONCAT('学生', LPAD(n, 2, '0')),
       'STUDENT',
       CONCAT('S2026', LPAD(n, 4, '0')),
       CONCAT('AI', IF(n <= 15, '1', '2'), '班'),
       CONCAT('1380000', LPAD(n, 4, '0'))
FROM seq;

-- 8 个环节时间窗：以当前时间为基准排布，保证系统一启动就能完整演示；
-- remark 记录流程说明文档中的原始时间点，学院管理员可在后台任意改期。
INSERT INTO stage_window (stage_code, stage_no, name, start_time, end_time, remark) VALUES
('BRIEFING',   1, '集中宣讲',         NOW() - INTERVAL 3 HOUR,                       NOW() - INTERVAL 2 HOUR,                        '文档：10月29日 14:00'),
('OPEN_DAY',   2, '实训室开放日',     NOW() - INTERVAL 2 HOUR,                       NOW() - INTERVAL 1 HOUR,                        '文档：10月29日—10月30日'),
('INTENT',     3, '意向填报',         NOW() - INTERVAL 20 MINUTE,                    NOW() + INTERVAL 4 HOUR,                        '文档：11月2日 08:00—12:00'),
('INTERVIEW',  4, '导师初选与面试考核', NOW() + INTERVAL 5 HOUR,                      NOW() + INTERVAL 1 DAY,                         '文档：11月2日 12:00—11月3日 17:00'),
('CONFIRM',    5, '拟录取与学生确认',  NOW() + INTERVAL 1 DAY,                        NOW() + INTERVAL 1 DAY + INTERVAL 4 HOUR,       '文档：11月4日 08:00—12:00'),
('SUPPLEMENT', 6, '分轮补录',         NOW() + INTERVAL 1 DAY + INTERVAL 4 HOUR,       NOW() + INTERVAL 2 DAY,                         '文档：11月4日 12:00—11月5日 17:00'),
('TRANSFER',   7, '统筹调剂录取',     NOW() + INTERVAL 2 DAY,                        NOW() + INTERVAL 2 DAY + INTERVAL 4 HOUR,       '文档：11月6日 08:00—12:00'),
('PUBLICITY',  8, '结果公示与正式入组', NOW() + INTERVAL 2 DAY + INTERVAL 4 HOUR,       NOW() + INTERVAL 3 DAY + INTERVAL 4 HOUR,       '文档：公示 1 天');

INSERT INTO supplement_round (round_no, open_flag) VALUES (2, 0), (3, 0);

INSERT INTO session_info (type, lab_id, title, start_time, end_time, place, capacity, content) VALUES
('BRIEFING', NULL, '学院集中宣讲', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 2 HOUR, '学院报告厅', 200, '介绍各实训室项目方向、招录名额、培养安排与考核要求。'),
('OPEN_DAY', 1, 'A室开放日', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, 'A室实验区', 60, '现场走访、与导师和高年级学生交流。'),
('OPEN_DAY', 2, 'B室开放日', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, 'B室实验区', 60, '机器人整机与嵌入式演示。'),
('OPEN_DAY', 3, 'C室开放日', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, 'C室机房', 60, '平台与数据方向项目展示。'),
('OPEN_DAY', 4, 'D室开放日', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, 'D室实验区', 60, '大模型应用与 Agent 演示。');

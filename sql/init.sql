-- =====================================================================
-- EatWhat 外卖平台 · 数据库初始化脚本
-- 依据 docs/04-数据库设计说明书.md 生成
-- 使用方法：mysql -u root -p < sql/init.sql
-- =====================================================================

DROP DATABASE IF EXISTS `eatwhat`;
CREATE DATABASE IF NOT EXISTS `eatwhat`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `eatwhat`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. 系统用户表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone`         VARCHAR(20)     NOT NULL                COMMENT '手机号',
  `password`      VARCHAR(100)    NOT NULL                COMMENT 'BCrypt密码哈希',
  `nickname`      VARCHAR(50)     DEFAULT NULL            COMMENT '昵称',
  `avatar`        VARCHAR(255)    DEFAULT NULL            COMMENT '头像URL',
  `role`          VARCHAR(20)     NOT NULL DEFAULT 'CONSUMER' COMMENT 'CONSUMER/MERCHANT/RIDER/ADMIN',
  `status`        TINYINT         NOT NULL DEFAULT 1      COMMENT '1正常 0禁用',
  `last_login_at` DATETIME        DEFAULT NULL            COMMENT '最后登录时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_phone` (`phone`),
  KEY `idx_sys_user_role` (`role`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- ---------------------------------------------------------------------
-- 2. 收货地址表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `contact_name` VARCHAR(50)     NOT NULL                COMMENT '联系人',
  `phone`        VARCHAR(20)     NOT NULL                COMMENT '联系电话',
  `province`     VARCHAR(50)     DEFAULT NULL            COMMENT '省',
  `city`         VARCHAR(50)     DEFAULT NULL            COMMENT '市',
  `district`     VARCHAR(50)     DEFAULT NULL            COMMENT '区',
  `detail`       VARCHAR(255)    NOT NULL                COMMENT '详细地址',
  `longitude`    DECIMAL(10,7)   DEFAULT NULL            COMMENT '经度',
  `latitude`     DECIMAL(10,7)   DEFAULT NULL            COMMENT '纬度',
  `is_default`   TINYINT         NOT NULL DEFAULT 0      COMMENT '是否默认 1是 0否',
  `tag`          VARCHAR(20)     DEFAULT NULL            COMMENT '地址标签 家/公司/学校',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_address_user_id` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户收货地址表';

-- ---------------------------------------------------------------------
-- 3. 商家信息表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `real_name`     VARCHAR(50)     NOT NULL                COMMENT '经营者姓名',
  `id_card`       VARCHAR(255)    DEFAULT NULL            COMMENT '身份证号(加密)',
  `license_img`   VARCHAR(255)    DEFAULT NULL            COMMENT '营业执照图片',
  `contact_phone` VARCHAR(20)     NOT NULL                COMMENT '联系电话',
  `audit_status`  TINYINT         NOT NULL DEFAULT 0      COMMENT '0待审核 1通过 2驳回',
  `audit_remark`  VARCHAR(255)    DEFAULT NULL            COMMENT '审核意见',
  `audited_at`    DATETIME        DEFAULT NULL            COMMENT '审核时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_user_id` (`user_id`),
  KEY `idx_merchant_audit` (`audit_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家信息表';

-- ---------------------------------------------------------------------
-- 4. 店铺表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id`      BIGINT UNSIGNED NOT NULL                COMMENT '商家ID',
  `name`             VARCHAR(100)    NOT NULL                COMMENT '店铺名称',
  `logo`             VARCHAR(255)    DEFAULT NULL            COMMENT '店铺Logo',
  `description`      VARCHAR(500)    DEFAULT NULL            COMMENT '店铺简介',
  `notice`           VARCHAR(255)    DEFAULT NULL            COMMENT '店铺公告',
  `address`          VARCHAR(255)    NOT NULL                COMMENT '店铺地址',
  `longitude`        DECIMAL(10,7)   DEFAULT NULL            COMMENT '经度',
  `latitude`         DECIMAL(10,7)   DEFAULT NULL            COMMENT '纬度',
  `phone`            VARCHAR(20)     NOT NULL                COMMENT '联系电话',
  `min_order_amount` DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '起送价',
  `delivery_fee`     DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '配送费',
  `delivery_fee_type` TINYINT        NOT NULL DEFAULT 0      COMMENT '配送费计费方式 0固定 1按距离阶梯',
  `packaging_fee`   DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '打包费',
  `delivery_range`   INT             NOT NULL DEFAULT 3000   COMMENT '配送范围(米)',
  `open_time`        TIME            DEFAULT NULL            COMMENT '开始营业时间',
  `close_time`       TIME            DEFAULT NULL            COMMENT '结束营业时间',
  `is_open`          TINYINT         NOT NULL DEFAULT 1      COMMENT '1营业 0打烊',
  `avg_score`        DECIMAL(3,2)    NOT NULL DEFAULT 5.00   COMMENT '平均评分',
  `month_sales`      INT             NOT NULL DEFAULT 0      COMMENT '月销量',
  `status`           TINYINT         NOT NULL DEFAULT 1      COMMENT '1正常 0冻结',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_merchant_id` (`merchant_id`),
  KEY `idx_shop_open` (`is_open`, `status`, `deleted`),
  KEY `idx_shop_score` (`avg_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='店铺表';

-- ---------------------------------------------------------------------
-- 5. 菜品分类表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `dish_category`;
CREATE TABLE `dish_category` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`    BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `name`       VARCHAR(50)     NOT NULL                COMMENT '分类名称',
  `sort`       INT             NOT NULL DEFAULT 0      COMMENT '排序值',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dish_category_shop` (`shop_id`, `deleted`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品分类表';

-- ---------------------------------------------------------------------
-- 6. 菜品表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`     BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `category_id` BIGINT UNSIGNED NOT NULL                COMMENT '分类ID',
  `name`        VARCHAR(100)    NOT NULL                COMMENT '菜品名称',
  `image`       VARCHAR(255)    DEFAULT NULL            COMMENT '图片URL',
  `description` VARCHAR(500)    DEFAULT NULL            COMMENT '描述',
  `price`       DECIMAL(10,2)   NOT NULL                COMMENT '单价',
  `stock`       INT             NOT NULL DEFAULT 0      COMMENT '库存(份)',
  `month_sales` INT             NOT NULL DEFAULT 0      COMMENT '月销量',
  `status`      TINYINT         NOT NULL DEFAULT 1      COMMENT '1在售 0下架 2售罄',
  `has_spec`    TINYINT         NOT NULL DEFAULT 0      COMMENT '是否有规格 0否 1是',
  `sort`        INT             NOT NULL DEFAULT 0      COMMENT '排序值',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dish_shop_status` (`shop_id`, `status`, `deleted`),
  KEY `idx_dish_category` (`category_id`, `deleted`, `sort`),
  KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品表';

-- ---------------------------------------------------------------------
-- 7. 购物车表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `shop_id`    BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `dish_id`    BIGINT UNSIGNED NOT NULL                COMMENT '菜品ID',
  `spec_id`    BIGINT UNSIGNED DEFAULT NULL            COMMENT '规格ID',
  `spec_name`  VARCHAR(50)     DEFAULT NULL            COMMENT '规格名称快照',
  `addon_ids`  VARCHAR(255)    DEFAULT NULL            COMMENT '加料ID,逗号分隔',
  `addon_names` VARCHAR(255)   DEFAULT NULL            COMMENT '加料名称快照',
  `addon_price` DECIMAL(10,2)  NOT NULL DEFAULT 0.00   COMMENT '加料合计价格',
  `unit_price` DECIMAL(10,2)   DEFAULT NULL            COMMENT '含规格与加料的单价',
  `quantity`   INT             NOT NULL DEFAULT 1      COMMENT '数量',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_cart_user_dish` (`user_id`, `dish_id`),
  KEY `idx_cart_user_shop` (`user_id`, `shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- ---------------------------------------------------------------------
-- 8. 订单表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`         VARCHAR(32)     NOT NULL                COMMENT '订单号',
  `user_id`          BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `shop_id`          BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `merchant_id`      BIGINT UNSIGNED NOT NULL                COMMENT '商家ID(冗余)',
  `rider_id`         BIGINT UNSIGNED DEFAULT NULL            COMMENT '骑手ID',
  `status`           TINYINT         NOT NULL DEFAULT 0      COMMENT '0待支付 1待接单 2备餐中 3待配送 4配送中 5已完成 6已取消 7退款中',
  `goods_amount`     DECIMAL(10,2)   NOT NULL                COMMENT '商品金额',
  `delivery_fee`     DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '配送费',
  `packaging_fee`   DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '打包费',
  `discount_amount`  DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '优惠金额',
  `total_amount`     DECIMAL(10,2)   NOT NULL                COMMENT '实付金额',
  `pay_type`         TINYINT         DEFAULT NULL            COMMENT '支付方式 1模拟支付',
  `pay_time`         DATETIME        DEFAULT NULL            COMMENT '支付时间',
  `trade_no`         VARCHAR(64)     DEFAULT NULL            COMMENT '第三方交易号',
  `receiver_name`    VARCHAR(50)     NOT NULL                COMMENT '收货人',
  `receiver_phone`   VARCHAR(20)     NOT NULL                COMMENT '收货电话',
  `receiver_address` VARCHAR(255)    NOT NULL                COMMENT '收货地址快照',
  `receiver_lng`      DECIMAL(10,7)   DEFAULT NULL                                         COMMENT '收货经度',
  `receiver_lat`      DECIMAL(10,7)   DEFAULT NULL                                         COMMENT '收货纬度',
  `remark`           VARCHAR(200)    DEFAULT NULL            COMMENT '订单备注',
  `tableware_count`  INT             NOT NULL DEFAULT 0      COMMENT '餐具份数',
  `cancel_reason`    VARCHAR(255)    DEFAULT NULL            COMMENT '取消原因',
  `completed_at`     DATETIME        DEFAULT NULL            COMMENT '完成时间',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_user` (`user_id`, `status`, `created_at`),
  KEY `idx_orders_shop` (`shop_id`, `status`, `created_at`),
  KEY `idx_orders_rider` (`rider_id`, `status`),
  KEY `idx_orders_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- ---------------------------------------------------------------------
-- 9. 订单明细表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`   BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
  `dish_id`    BIGINT UNSIGNED NOT NULL                COMMENT '菜品ID',
  `dish_name`  VARCHAR(100)    NOT NULL                COMMENT '菜品名称快照',
  `spec_name`  VARCHAR(50)     DEFAULT NULL            COMMENT '规格名称快照',
  `addon_names` VARCHAR(255)   DEFAULT NULL            COMMENT '加料名称快照',
  `addon_price` DECIMAL(10,2)  NOT NULL DEFAULT 0.00   COMMENT '加料合计价格',
  `dish_image` VARCHAR(255)    DEFAULT NULL            COMMENT '菜品图片快照',
  `price`      DECIMAL(10,2)   NOT NULL                COMMENT '单价快照',
  `quantity`   INT             NOT NULL                COMMENT '数量',
  `subtotal`   DECIMAL(10,2)   NOT NULL                COMMENT '小计',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

-- ---------------------------------------------------------------------
-- 10. 订单状态日志表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `order_status_log`;
CREATE TABLE `order_status_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`      BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
  `from_status`   TINYINT         DEFAULT NULL            COMMENT '原状态(首次为NULL)',
  `to_status`     TINYINT         NOT NULL                COMMENT '新状态',
  `operator_id`   BIGINT UNSIGNED DEFAULT NULL            COMMENT '操作者ID(0为系统)',
  `operator_role` VARCHAR(20)     DEFAULT NULL            COMMENT '操作者角色',
  `remark`        VARCHAR(255)    DEFAULT NULL            COMMENT '备注',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_log_order` (`order_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单状态日志表';

-- ---------------------------------------------------------------------
-- 11. 配送任务表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `delivery_task`;
CREATE TABLE `delivery_task` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`         BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
  `shop_id`          BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `rider_id`         BIGINT UNSIGNED DEFAULT NULL            COMMENT '骑手ID',
  `status`           TINYINT         NOT NULL DEFAULT 0      COMMENT '0待抢单 1已接单 2已取餐 3已送达',
  `pickup_address`   VARCHAR(255)    NOT NULL                COMMENT '取餐地址',
  `delivery_address` VARCHAR(255)    NOT NULL                COMMENT '送达地址',
  `distance`         DECIMAL(8,2)    DEFAULT NULL            COMMENT '距离(公里)',
  `fee`              DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '骑手收益',
  `grabbed_at`       DATETIME        DEFAULT NULL            COMMENT '抢单时间',
  `picked_at`        DATETIME        DEFAULT NULL            COMMENT '取餐时间',
  `delivered_at`     DATETIME        DEFAULT NULL            COMMENT '送达时间',
  `estimated_arrival_at` DATETIME    DEFAULT NULL            COMMENT '预计送达时间',
  `on_time`          TINYINT         DEFAULT NULL            COMMENT '是否准时送达 1准时 0超时 NULL未送达',
  `version`          INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_order_id` (`order_id`),
  KEY `idx_delivery_status` (`status`, `created_at`),
  KEY `idx_delivery_rider` (`rider_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送任务表';

-- ---------------------------------------------------------------------
-- 12. 骑手信息表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `rider`;
CREATE TABLE `rider` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `real_name`     VARCHAR(50)     NOT NULL                COMMENT '真实姓名',
  `id_card`       VARCHAR(255)    DEFAULT NULL            COMMENT '身份证号(加密)',
  `health_img`    VARCHAR(255)    DEFAULT NULL            COMMENT '健康证图片',
  `health_expire` DATE            DEFAULT NULL            COMMENT '健康证有效期至',
  `contact_phone` VARCHAR(20)     NOT NULL                COMMENT '联系电话',
  `service_area`  VARCHAR(200)    DEFAULT NULL            COMMENT '常用配送区域',
  `accepting`     TINYINT         NOT NULL DEFAULT 1      COMMENT '1接单中 0已暂停',
  `max_distance`  DECIMAL(4,1)    NOT NULL DEFAULT 5.0    COMMENT '接单半径(公里)',
  `max_orders`    INT             NOT NULL DEFAULT 5      COMMENT '最大同时接单数',
  `audit_status`  TINYINT         NOT NULL DEFAULT 0      COMMENT '0待审核 1通过 2驳回',
  `audit_remark`  VARCHAR(255)    DEFAULT NULL            COMMENT '审核意见',
  `audited_at`    DATETIME        DEFAULT NULL            COMMENT '审核时间',
  `total_orders`  INT             NOT NULL DEFAULT 0      COMMENT '累计配送单量',
  `total_income`  DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '累计收益',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rider_user_id` (`user_id`),
  KEY `idx_rider_audit` (`audit_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='骑手信息表';

-- ---------------------------------------------------------------------
-- 13. 评价表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`       BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
  `user_id`        BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `shop_id`        BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `rider_id`       BIGINT UNSIGNED DEFAULT NULL            COMMENT '骑手ID',
  `shop_score`     TINYINT         NOT NULL DEFAULT 5      COMMENT '店铺评分1-5',
  `delivery_score` TINYINT         NOT NULL DEFAULT 5      COMMENT '配送评分1-5',
  `content`        VARCHAR(500)    DEFAULT NULL            COMMENT '评价内容',
  `images`         VARCHAR(1000)   DEFAULT NULL            COMMENT '图片URL逗号分隔',
  `is_anonymous`   TINYINT         NOT NULL DEFAULT 0      COMMENT '是否匿名',
  `reply`          VARCHAR(500)    DEFAULT NULL            COMMENT '商家回复',
  `replied_at`     DATETIME        DEFAULT NULL            COMMENT '回复时间',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted`        TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_review_order_id` (`order_id`),
  KEY `idx_review_shop` (`shop_id`, `created_at`),
  KEY `idx_review_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评价表';

-- ---------------------------------------------------------------------
-- 14. 店铺收藏表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `shop_favorite`;
CREATE TABLE `shop_favorite` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `shop_id`    BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_shop` (`user_id`, `shop_id`),
  KEY `idx_favorite_user` (`user_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='店铺收藏表';

-- ---------------------------------------------------------------------
-- 15. 操作日志表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id`    BIGINT UNSIGNED NOT NULL                COMMENT '操作人ID',
  `operator_role`  VARCHAR(20)     NOT NULL                COMMENT '操作人角色',
  `module`         VARCHAR(50)     NOT NULL                COMMENT '模块',
  `action`         VARCHAR(100)    NOT NULL                COMMENT '操作描述',
  `target_type`    VARCHAR(50)     DEFAULT NULL            COMMENT '目标类型',
  `target_id`      BIGINT UNSIGNED DEFAULT NULL            COMMENT '目标ID',
  `request_method` VARCHAR(10)     DEFAULT NULL            COMMENT 'HTTP方法',
  `request_uri`    VARCHAR(255)    DEFAULT NULL            COMMENT '请求路径',
  `request_params` TEXT            DEFAULT NULL            COMMENT '请求参数(脱敏)',
  `ip`             VARCHAR(64)     DEFAULT NULL            COMMENT '客户端IP',
  `result`         TINYINT         NOT NULL DEFAULT 1      COMMENT '1成功 0失败',
  `error_msg`      VARCHAR(500)    DEFAULT NULL            COMMENT '错误信息',
  `cost_ms`        INT             DEFAULT NULL            COMMENT '耗时(ms)',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_oplog_operator` (`operator_id`, `created_at`),
  KEY `idx_oplog_module` (`module`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ---------------------------------------------------------------------
-- 16. 售后申请表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `after_sale`;
CREATE TABLE `after_sale` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `after_sale_no`       VARCHAR(32)     NOT NULL                COMMENT '售后单号',
  `order_id`            BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
  `order_no`            VARCHAR(32)     NOT NULL                COMMENT '订单号冗余',
  `user_id`             BIGINT UNSIGNED NOT NULL                COMMENT '顾客ID',
  `shop_id`             BIGINT UNSIGNED NOT NULL                COMMENT '店铺ID',
  `merchant_id`         BIGINT UNSIGNED NOT NULL                COMMENT '商家ID冗余',
  `type`                TINYINT         NOT NULL DEFAULT 1      COMMENT '1退款 2申诉',
  `reason`              VARCHAR(500)    NOT NULL                COMMENT '申请原因',
  `images`              VARCHAR(1000)   DEFAULT NULL            COMMENT '凭证图片,逗号分隔',
  `amount`              DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '申请退款金额',
  `status`              TINYINT         NOT NULL DEFAULT 0      COMMENT '0待商家处理 1商家同意 2商家拒绝 3已撤销 4待平台仲裁 5支持顾客 6支持商家',
  `merchant_reply`      VARCHAR(500)    DEFAULT NULL            COMMENT '商家处理意见',
  `merchant_handled_at` DATETIME        DEFAULT NULL            COMMENT '商家处理时间',
  `admin_verdict`       VARCHAR(500)    DEFAULT NULL            COMMENT '平台裁定说明',
  `admin_id`            BIGINT UNSIGNED DEFAULT NULL            COMMENT '裁定管理员ID',
  `admin_verdict_at`    DATETIME        DEFAULT NULL            COMMENT '裁定时间',
  `refunded_at`         DATETIME        DEFAULT NULL            COMMENT '退款完成时间',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  KEY `idx_after_sale_order` (`order_id`),
  KEY `idx_after_sale_user` (`user_id`, `status`),
  KEY `idx_after_sale_merchant` (`merchant_id`, `status`),
  KEY `idx_after_sale_status` (`status`),
  KEY `idx_after_sale_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='售后申请';

-- ---------------------------------------------------------------------
-- 17. 用户消息表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_message`;
CREATE TABLE `user_message` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT UNSIGNED NOT NULL                COMMENT '接收人ID',
  `type`       TINYINT         NOT NULL DEFAULT 1      COMMENT '1订单消息 2系统公告 3售后消息',
  `title`      VARCHAR(100)    NOT NULL                COMMENT '标题',
  `content`    VARCHAR(500)    NOT NULL                COMMENT '内容',
  `order_id`   BIGINT UNSIGNED DEFAULT NULL            COMMENT '关联订单ID',
  `order_no`   VARCHAR(32)     DEFAULT NULL            COMMENT '关联订单号',
  `biz_id`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '关联业务ID(售后单等)',
  `is_read`    TINYINT         NOT NULL DEFAULT 0      COMMENT '0未读 1已读',
  `read_at`    DATETIME        DEFAULT NULL            COMMENT '阅读时间',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_msg_user_read` (`user_id`, `is_read`, `created_at`),
  KEY `idx_msg_user_type` (`user_id`, `type`, `created_at`),
  KEY `idx_msg_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户消息';

-- ---------------------------------------------------------------------
-- 18. 平台公告表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(100)    NOT NULL                COMMENT '公告标题',
  `content`       TEXT            NOT NULL                COMMENT '公告正文',
  `type`          TINYINT         NOT NULL DEFAULT 1      COMMENT '1系统公告 2平台活动 3维护通知',
  `target_role`   VARCHAR(20)     NOT NULL DEFAULT 'ALL'  COMMENT '可见范围 ALL/CONSUMER/MERCHANT/RIDER',
  `start_time`    DATETIME        NOT NULL                COMMENT '生效时间',
  `end_time`      DATETIME        DEFAULT NULL            COMMENT '失效时间,为空表示长期有效',
  `status`        TINYINT         NOT NULL DEFAULT 1      COMMENT '0已下线 1已发布',
  `publisher_id`  BIGINT UNSIGNED NOT NULL                COMMENT '发布管理员ID',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_announcement_status` (`status`, `start_time`),
  KEY `idx_announcement_role` (`target_role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台公告';

-- ---------------------------------------------------------------------
-- 19. 敏感词库表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word`       VARCHAR(50)     NOT NULL                COMMENT '敏感词',
  `category`   VARCHAR(20)     NOT NULL DEFAULT 'ALL'  COMMENT '适用范围 ALL/REVIEW/REMARK/NICKNAME',
  `status`     TINYINT         NOT NULL DEFAULT 1      COMMENT '0已停用 1已启用',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sensitive_word` (`word`),
  KEY `idx_sensitive_status` (`status`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='敏感词库';

-- ---------------------------------------------------------------------
-- 20. 平台参数表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   VARCHAR(64)     NOT NULL                COMMENT '参数键',
  `config_value` VARCHAR(500)    NOT NULL                COMMENT '参数值',
  `config_name`  VARCHAR(100)    NOT NULL                COMMENT '参数名称',
  `config_group` VARCHAR(50)     NOT NULL DEFAULT 'PLATFORM' COMMENT '参数分组 ORDER/DELIVERY/PLATFORM',
  `remark`       VARCHAR(255)    DEFAULT NULL            COMMENT '说明',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_config_key` (`config_key`),
  KEY `idx_sys_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台参数';

-- ---------------------------------------------------------------------
-- 21. 配送异常与转单申请表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `delivery_exception`;
CREATE TABLE `delivery_exception` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`       BIGINT UNSIGNED NOT NULL                COMMENT '配送任务ID',
  `order_id`      BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
  `order_no`      VARCHAR(32)     DEFAULT NULL            COMMENT '订单号冗余',
  `rider_id`      BIGINT UNSIGNED NOT NULL                COMMENT '骑手ID',
  `type`          TINYINT         NOT NULL DEFAULT 2      COMMENT '1转单申请 2异常上报',
  `reason_type`   TINYINT         NOT NULL DEFAULT 6      COMMENT '1联系不上顾客 2地址错误 3餐品破损 4车辆故障 5天气原因 6其他',
  `description`   VARCHAR(500)    DEFAULT NULL            COMMENT '情况说明',
  `images`        VARCHAR(1000)   DEFAULT NULL            COMMENT '凭证图片,逗号分隔',
  `status`        TINYINT         NOT NULL DEFAULT 0      COMMENT '0待处理 1已处理',
  `handle_remark` VARCHAR(500)    DEFAULT NULL            COMMENT '处理说明',
  `handled_at`    DATETIME        DEFAULT NULL            COMMENT '处理时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_exception_task` (`task_id`),
  KEY `idx_exception_rider` (`rider_id`, `type`),
  KEY `idx_exception_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送异常与转单申请';

-- ---------------------------------------------------------------------
-- 22. 数据字典表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type`  VARCHAR(64)     NOT NULL                COMMENT '字典类型',
  `dict_label` VARCHAR(100)    NOT NULL                COMMENT '字典标签（展示文案）',
  `dict_value` VARCHAR(64)     NOT NULL                COMMENT '字典值',
  `sort`       INT             NOT NULL DEFAULT 0      COMMENT '排序，越小越前',
  `status`     TINYINT         NOT NULL DEFAULT 1      COMMENT '0停用 1启用',
  `remark`     VARCHAR(255)    DEFAULT NULL            COMMENT '备注',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_type_value` (`dict_type`, `dict_value`),
  KEY `idx_sys_dict_type` (`dict_type`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据字典';
-- ---------------------------------------------------------------------
-- 23. 每日统计汇总表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `stats_daily`;
CREATE TABLE `stats_daily` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date`       DATE            NOT NULL                COMMENT '统计日期',
  `order_count`     INT             NOT NULL DEFAULT 0      COMMENT '订单量',
  `completed_count` INT             NOT NULL DEFAULT 0      COMMENT '完成单量',
  `cancel_count`    INT             NOT NULL DEFAULT 0      COMMENT '取消单量',
  `total_amount`    DECIMAL(12,2)   NOT NULL DEFAULT 0.00   COMMENT '交易额（已支付订单）',
  `refund_amount`   DECIMAL(12,2)   NOT NULL DEFAULT 0.00   COMMENT '退款额',
  `new_users`       INT             NOT NULL DEFAULT 0      COMMENT '新增用户数',
  `new_merchants`   INT             NOT NULL DEFAULT 0      COMMENT '新增商家数',
  `new_riders`      INT             NOT NULL DEFAULT 0      COMMENT '新增骑手数',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stats_daily_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日统计汇总表';

-- ---------------------------------------------------------------------
-- 23. 菜品规格表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `dish_spec`;
CREATE TABLE `dish_spec` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id`    BIGINT UNSIGNED NOT NULL                COMMENT '菜品ID',
  `name`       VARCHAR(50)     NOT NULL                COMMENT '规格名称,如大份/中份/小份',
  `price`      DECIMAL(10,2)   NOT NULL                COMMENT '规格价格',
  `is_default` TINYINT         NOT NULL DEFAULT 0      COMMENT '是否默认 0否 1是',
  `sort`       INT             NOT NULL DEFAULT 0      COMMENT '排序',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dish_spec_dish` (`dish_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品规格表';

-- ---------------------------------------------------------------------
-- 24. 菜品加料表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `dish_addon`;
CREATE TABLE `dish_addon` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id`    BIGINT UNSIGNED NOT NULL                COMMENT '菜品ID',
  `name`       VARCHAR(50)     NOT NULL                COMMENT '加料名称,如加米饭/加卤蛋',
  `price`      DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '加料价格',
  `sort`       INT             NOT NULL DEFAULT 0      COMMENT '排序',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dish_addon_dish` (`dish_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品加料表';
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 初始化数据
-- 统一密码（BCrypt）：
--   ADMIN    13800000000 / Admin@123
--   CONSUMER 13800000001 / User@123   （张三）
--   CONSUMER 13800000002 / User@123   （李四）
--   MERCHANT 13800000010 / Merchant@123（川味小厨）
--   MERCHANT 13800000011 / Merchant@123（麦香烘焙）
--   RIDER    13800000020 / Rider@123  （王骑手）
--   RIDER    13800000021 / Rider@123  （赵骑手）
-- =====================================================================

INSERT INTO `sys_user` (`id`, `phone`, `password`, `nickname`, `avatar`, `role`, `status`) VALUES
(1, '13800000000', '$2a$10$M4ssfs.9LSknqaxElPVAoeset3Jw8jfutmYuXwIVvyZ52c1zR2M/6',   '平台管理员', NULL, 'ADMIN',    1),
(2, '13800000001', '$2a$10$2B3PB.UqEuZzORq7GxVAaegiss/6Rckcnyjd5Z00FEq4XYN.ZACEe',    '张三',       NULL, 'CONSUMER', 1),
(3, '13800000002', '$2a$10$2B3PB.UqEuZzORq7GxVAaegiss/6Rckcnyjd5Z00FEq4XYN.ZACEe',    '李四',       NULL, 'CONSUMER', 1),
(4, '13800000010', '$2a$10$0.tTNqTv2lADJJmLGdrIhuJTPFmY8UghrQ4fq0c3iCETlTLTtlNW2','川味小厨',    NULL, 'MERCHANT', 1),
(5, '13800000011', '$2a$10$0.tTNqTv2lADJJmLGdrIhuJTPFmY8UghrQ4fq0c3iCETlTLTtlNW2','麦香烘焙',    NULL, 'MERCHANT', 1),
(6, '13800000020', '$2a$10$tBYCLXPdGGer8ARtuUypIuwPEcmi8zodKj9nfsHpsZ7XfrPOysx66',   '王骑手',     NULL, 'RIDER',    1),
(7, '13800000021', '$2a$10$tBYCLXPdGGer8ARtuUypIuwPEcmi8zodKj9nfsHpsZ7XfrPOysx66',   '赵骑手',     NULL, 'RIDER',    1),
(8, '13800000012', '$2a$10$0.tTNqTv2lADJJmLGdrIhuJTPFmY8UghrQ4fq0c3iCETlTLTtlNW2','轻食小铺',    NULL, 'MERCHANT', 1);

INSERT INTO `user_address` (`id`, `user_id`, `contact_name`, `phone`, `province`, `city`, `district`, `detail`, `longitude`, `latitude`, `is_default`) VALUES
(1, 2, '张三', '13800000001', '浙江省', '杭州市', '西湖区', '文三路 100 号 3 幢 501 室', 120.1234560, 30.2765000, 1),
(2, 2, '张三', '13800000001', '浙江省', '杭州市', '滨江区', '江南大道 588 号 A 座 1201', 120.2100000, 30.2080000, 0),
(3, 3, '李四', '13800000002', '浙江省', '杭州市', '西湖区', '文三路 200 号 1 幢 802 室', 120.1200000, 30.2780000, 1);

INSERT INTO `merchant` (`id`, `user_id`, `real_name`, `id_card`, `license_img`, `contact_phone`, `audit_status`, `audit_remark`, `audited_at`) VALUES
(1, 4, '陈建国', '330106199001011234', NULL, '13800000010', 1, '资质齐全，审核通过', NOW()),
(2, 5, '周麦香', '330106199205055678', NULL, '13800000011', 1, '资质齐全，审核通过', NOW()),
(3, 8, '林轻食', '330106199308089012', NULL, '13800000012', 1, '资质齐全，审核通过', NOW());

INSERT INTO `shop` (`id`, `merchant_id`, `name`, `logo`, `description`, `notice`, `address`, `longitude`, `latitude`, `phone`, `min_order_amount`, `delivery_fee`, `delivery_fee_type`, `delivery_range`, `open_time`, `close_time`, `is_open`, `avg_score`, `month_sales`, `status`) VALUES
(1, 1, '川味小厨', NULL, '正宗川菜，麻辣鲜香，现炒现做', '本店所有菜品均使用非转基因食用油', '浙江省杭州市西湖区文三路 88 号 1 层', 120.1250000, 30.2770000, '13800000010', 20.00, 3.00, 1, 3000, '09:00:00', '21:00:00', 1, 4.80, 1268, 1),
(2, 2, '麦香烘焙', NULL, '手作面包与甜点，每日现烤', '下午 3 点后全场 8 折', '浙江省杭州市西湖区学院路 200 号', 120.1300000, 30.2800000, '13800000011', 15.00, 2.00, 0, 2500, '08:00:00', '20:00:00', 1, 4.60, 892, 1),
(3, 3, '麦香轻食', NULL, '轻食沙拉与低卡套餐', '支持预订次日餐', '浙江省杭州市西湖区教工路 55 号', 120.1350000, 30.2850000, '13800000012', 18.00, 2.50, 1, 2000, '10:00:00', '19:00:00', 1, 4.50, 431, 1);

INSERT INTO `dish_category` (`id`, `shop_id`, `name`, `sort`) VALUES
(1, 1, '招牌热菜', 1),
(2, 1, '主食米饭', 2),
(3, 1, '汤羹饮品', 3),
(4, 2, '现烤面包', 1),
(5, 2, '甜点蛋糕', 2),
(6, 2, '咖啡奶茶', 3),
(7, 3, '轻食沙拉', 1),
(8, 3, '低卡套餐', 2);

INSERT INTO `dish` (`id`, `shop_id`, `category_id`, `name`, `image`, `description`, `price`, `stock`, `month_sales`, `status`, `sort`) VALUES
(1,  1, 1, '宫保鸡丁', NULL, '花生米脆，鸡肉嫩，微辣回甜', 28.00, 100, 320, 1, 1),
(2,  1, 1, '麻婆豆腐', NULL, '嫩豆腐配牛肉末，麻辣鲜香', 22.00, 100, 268, 1, 2),
(3,  1, 1, '水煮牛肉', NULL, '精选牛里脊，麻辣过瘾', 42.00, 60,  185, 1, 3),
(4,  1, 1, '鱼香肉丝', NULL, '经典川菜，酸甜微辣', 26.00, 100, 210, 1, 4),
(5,  1, 2, '米饭（大份）', NULL, '东北珍珠米，现蒸软糯', 3.00,  500, 1350, 1, 1),
(6,  1, 2, '米饭（小份）', NULL, '东北珍珠米，现蒸软糯', 2.00,  500, 980,  1, 2),
(7,  1, 3, '番茄蛋花汤', NULL, '西红柿鸡蛋汤，清爽解辣', 8.00,  100, 300,  1, 1),
(8,  1, 3, '酸梅汤', NULL, '自制古法酸梅汤，冰镇', 6.00,  200, 420,  1, 2),
(9,  2, 4, '原味吐司', NULL, '低温发酵 18 小时，麦香浓郁', 15.00, 80,  260,  1, 1),
(10, 2, 4, '可颂牛角包', NULL, '36 层酥皮，黄油香浓', 12.00, 60,  310,  1, 2),
(11, 2, 5, '提拉米苏', NULL, '马斯卡彭奶酪，手指饼干', 26.00, 40,  178,  1, 1),
(12, 2, 5, '草莓慕斯', NULL, '当季奶油草莓制作', 28.00, 30,  120,  1, 2),
(13, 2, 6, '美式咖啡', NULL, '意式浓缩兑水，醇厚不酸', 15.00, 100, 460,  1, 1),
(14, 2, 6, '珍珠奶茶', NULL, '手煮黑糖珍珠', 16.00, 100, 520,  1, 2),
(15, 3, 7, '鸡胸肉沙拉', NULL, '低温慢煮鸡胸，油醋汁', 25.00, 50,  150,  1, 1),
(16, 3, 7, '牛油果藜麦沙拉', NULL, '藜麦牛油果，膳食纤维丰富', 32.00, 40,  98,   1, 2),
(17, 3, 8, '低卡轻食套餐 A', NULL, '鸡胸沙拉 + 全麦面包 + 无糖豆浆', 38.00, 30, 88, 1, 1),
(18, 3, 8, '低卡轻食套餐 B', NULL, '牛油果沙拉 + 水煮蛋 + 黑咖啡', 42.00, 30, 66, 1, 2);

-- 菜品规格与加料种子数据（米饭支持大份/中份/小份与加料）
UPDATE `dish` SET `has_spec` = 1 WHERE `id` IN (5, 6);

INSERT INTO `dish_spec` (`dish_id`, `name`, `price`, `is_default`, `sort`) VALUES
(5, '大份', 3.00, 1, 0),
(5, '中份', 2.50, 0, 1),
(5, '小份', 2.00, 0, 2),
(6, '大份', 2.00, 1, 0),
(6, '小份', 1.50, 0, 1);

INSERT INTO `dish_addon` (`dish_id`, `name`, `price`, `sort`) VALUES
(5, '加米饭', 1.00, 0),
(5, '加卤蛋', 2.00, 1),
(5, '加青菜', 1.50, 2),
(6, '加米饭', 1.00, 0),
(6, '加卤蛋', 2.00, 1);
INSERT INTO `rider` (`id`, `user_id`, `real_name`, `id_card`, `health_img`, `contact_phone`, `audit_status`, `audit_remark`, `audited_at`, `total_orders`, `total_income`) VALUES
(1, 6, '王强', '330106199303031234', NULL, '13800000020', 1, '健康证有效，审核通过', NOW(), 0, 0.00),
(2, 7, '赵敏', '330106199407075678', NULL, '13800000021', 1, '健康证有效，审核通过', NOW(), 0, 0.00);

INSERT INTO `shop_favorite` (`id`, `user_id`, `shop_id`) VALUES
(1, 2, 1),
(2, 2, 2),
(3, 3, 3);

INSERT INTO `announcement` (`id`, `title`, `content`, `type`, `target_role`, `start_time`, `end_time`, `status`, `publisher_id`) VALUES
(1, '欢迎使用 EatWhat 外卖平台', 'EatWhat 外卖平台正式上线啦！下单即享新用户立减优惠，米饭管够～', 1, 'ALL', '2026-10-01 00:00:00', NULL, 1, 1),
(2, '双十一米饭节活动', '11 月 11 日当天，全平台米饭类菜品 5 折，米饭自由就在今天！', 2, 'CONSUMER', '2026-11-01 00:00:00', '2026-11-12 00:00:00', 1, 1),
(3, '系统维护通知', '平台将于每周一凌晨 02:00-03:00 进行例行维护，期间下单功能可能短暂不可用。', 3, 'ALL', '2026-10-01 00:00:00', NULL, 1, 1);

INSERT INTO `sensitive_word` (`id`, `word`, `category`, `status`) VALUES
(1, '傻逼', 'ALL', 1),
(2, '垃圾', 'REVIEW', 1),
(3, '骗子', 'REVIEW', 1),
(4, '刷单', 'REMARK', 1);

INSERT INTO `sys_dict` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`, `remark`) VALUES
(1,  'order_status',         '待支付',     '0', 1, 1, '订单状态字典'),
(2,  'order_status',         '待接单',     '1', 2, 1, NULL),
(3,  'order_status',         '备餐中',     '2', 3, 1, NULL),
(4,  'order_status',         '待配送',     '3', 4, 1, NULL),
(5,  'order_status',         '配送中',     '4', 5, 1, NULL),
(6,  'order_status',         '已完成',     '5', 6, 1, NULL),
(7,  'order_status',         '已取消',     '6', 7, 1, NULL),
(8,  'order_status',         '退款中',     '7', 8, 1, NULL),
(9,  'dish_status',          '已下架',     '0', 1, 1, '菜品状态字典'),
(10, 'dish_status',          '在售',       '1', 2, 1, NULL),
(11, 'dish_status',          '售罄',       '2', 3, 1, NULL),
(12, 'audit_status',         '待审核',     '0', 1, 1, '审核状态字典'),
(13, 'audit_status',         '已通过',     '1', 2, 1, NULL),
(14, 'audit_status',         '已驳回',     '2', 3, 1, NULL),
(15, 'delivery_task_status', '待抢单',     '0', 1, 1, '配送任务状态字典'),
(16, 'delivery_task_status', '已接单',     '1', 2, 1, NULL),
(17, 'delivery_task_status', '已取餐',     '2', 3, 1, NULL),
(18, 'delivery_task_status', '已送达',     '3', 4, 1, NULL),
(19, 'after_sale_status',    '待商家处理', '0', 1, 1, '售后状态字典'),
(20, 'after_sale_status',    '商家已同意', '1', 2, 1, NULL),
(21, 'after_sale_status',    '商家已拒绝', '2', 3, 1, NULL),
(22, 'after_sale_status',    '已撤销',     '3', 4, 1, NULL),
(23, 'after_sale_status',    '待平台仲裁', '4', 5, 1, NULL),
(24, 'after_sale_status',    '支持顾客',   '5', 6, 1, NULL),
(25, 'after_sale_status',    '支持商家',   '6', 7, 1, NULL),
(26, 'user_role',            '消费者',     'CONSUMER', 1, 1, '用户角色字典'),
(27, 'user_role',            '商家',       'MERCHANT', 2, 1, NULL),
(28, 'user_role',            '骑手',       'RIDER',    3, 1, NULL),
(29, 'user_role',            '管理员',     'ADMIN',    4, 1, NULL);
INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `config_name`, `config_group`, `remark`) VALUES
(1, 'order.pay.timeout.minutes', '15', '订单支付超时时间（分钟）', 'ORDER', '超过该时长未支付订单自动取消'),
(2, 'order.review.valid.days', '7', '订单评价有效期（天）', 'ORDER', '订单完成后多少天内可评价'),
(3, 'delivery.fee.base.km', '3', '阶梯配送费基础里程（公里）', 'DELIVERY', '该里程内按店铺基础配送费收取'),
(4, 'delivery.fee.extra.per.km', '1', '阶梯配送费超出加价（元/公里）', 'DELIVERY', '超出基础里程后每公里加收金额'),
(5, 'platform.service.phone', '400-800-8888', '平台客服电话', 'PLATFORM', '展示在顾客端与骑手端'),
(6, 'platform.min.withdraw', '10', '骑手最低提现金额（元）', 'PLATFORM', '低于该金额不可提现');

-- =====================================================================
-- 说明：以上账号密码均以 BCrypt 密文入库，明文与哈希对应关系如下：
--   13800000000 / Admin@123     管理员
--   13800000001 / User@123      消费者（张三）
--   13800000002 / User@123      消费者（李四）
--   13800000010 / Merchant@123  商家（川味小厨）
--   13800000011 / Merchant@123  商家（麦香烘焙）
--   13800000012 / Merchant@123  商家（麦香轻食）
--   13800000020 / Rider@123     骑手（王强）
--   13800000021 / Rider@123     骑手（赵敏）
-- 如需重新生成哈希，可执行：
--   cd backend && mvn -q exec:java -Dexec.mainClass=com.eatwhat.common.utils.BcryptGen
-- =====================================================================

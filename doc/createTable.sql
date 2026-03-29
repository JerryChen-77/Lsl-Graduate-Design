create database xxx;

use xxx;

-- auto-generated definition
create table chat_history
(
    id          bigint auto_increment comment 'id'
        primary key,
    message     longtext                           not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    chatId      bigint                             not null comment '对话id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    fatherId    bigint                             null comment '初始问题(仅ai回答会有father_id)',
    fingerprint varchar(512)                       null comment '指纹'
)
    comment '对话历史' collate = utf8mb4_unicode_ci;

create index idx_chatId
    on chat_history (chatId);

create index idx_chatId_createTime
    on chat_history (chatId, createTime);

create index idx_createTime
    on chat_history (createTime);


-- auto-generated definition
create table chat_history_original
(
    id          bigint auto_increment comment 'id'
        primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai/toolExecutionRequest/toolExecutionResult',
    chatId      bigint                             not null comment '对话id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment '对话历史' collate = utf8mb4_unicode_ci;

create index idx_appId
    on chat_history_original (chatId);

create index idx_appId_createTime
    on chat_history_original (chatId, createTime);

create index idx_createTime
    on chat_history_original (createTime);


-- auto-generated definition
create table chat_topic
(
    id         bigint auto_increment comment '对话主题id'
        primary key,
    content    varchar(256)                       not null comment '对话内容概括',
    userId     bigint                             not null comment '创建用户id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
)
    comment '对话主题表' collate = utf8mb4_unicode_ci;

-- auto-generated definition
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    constraint uk_userAccount
        unique (userAccount)
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userName
    on user (userName);


-- auto-generated definition
create table question_fingerprint
(
    id             bigint auto_increment comment 'id'
        primary key,
    fingerprint    varchar(128)                            not null comment '问题指纹(SHA256/SimHash)',
    normalizedText text                                    not null comment '标准化后的问题文本',
    cachedAnswer   longtext                                null comment '缓存的答案内容(仅status=1时有值)',
    status         tinyint       default 0                 not null comment '状态:0-待评分,1-已缓存,2-不合格,3-已过期',
    avgRating      decimal(3, 2) default 0.00              null comment '当前平均评分',
    totalRatings   int           default 0                 not null comment '总评分次数',
    lastAskedTime  datetime                                null comment '最后被问时间',
    askCount       int           default 1                 not null comment '被问总次数',
    createTime     datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint       default 0                 not null comment '是否删除',
    constraint uk_fingerprint
        unique (fingerprint)
)
    comment '问题指纹表' collate = utf8mb4_unicode_ci;

create index idx_status
    on question_fingerprint (status);


-- auto-generated definition
create table rating_feedback
(
    id          bigint auto_increment comment 'id'
        primary key,
    fingerprint varchar(128)                       not null comment '问题指纹',
    userId      bigint                             not null comment '评分用户ID',
    chatId      bigint                             null comment '对话id',
    rating      tinyint                            not null comment '评分:1-5分',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment '评分反馈表' collate = utf8mb4_unicode_ci;

create index idx_fingerprint
    on rating_feedback (fingerprint);

create index idx_user_id
    on rating_feedback (userId);


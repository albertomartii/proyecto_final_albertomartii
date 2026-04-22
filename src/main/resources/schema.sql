CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(100),
                       email VARCHAR(200),
                       role VARCHAR(50)
);

CREATE TABLE garage (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(200),
                        location VARCHAR(200),
                        owner_id BIGINT
);

CREATE TABLE parts (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(200),
                       type VARCHAR(50),
                       hp_delta DOUBLE,
                       torque_delta DOUBLE,
                       grip_delta DOUBLE,
                       weight_delta DOUBLE
);

CREATE TABLE cars (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      make VARCHAR(100),
                      model VARCHAR(100),
                      year INT,
                      base_power DOUBLE,
                      base_torque DOUBLE,
                      mass DOUBLE,
                      grip_base DOUBLE,
                      weight_distribution_front DOUBLE,
                      garage_id BIGINT
);

CREATE TABLE car_parts (
                           car_id BIGINT,
                           part_id BIGINT,
                           PRIMARY KEY (car_id, part_id)
);

CREATE TABLE build_history (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               car_id BIGINT,
                               owner_id BIGINT,
                               timestamp TIMESTAMP,
                               parts_snapshot_json CLOB,
                               target_discipline VARCHAR(50)
);

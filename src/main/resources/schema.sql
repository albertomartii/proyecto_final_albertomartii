-- ============================================================
-- DS Racing Garage - Schema H2 (in-memory)
-- IMPORTANTE: year, name, type, timestamp son palabras
-- reservadas en H2 y deben ir entre comillas dobles.
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
                                     id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(100),
    email    VARCHAR(200),
    role     VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS garage (
                                      id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      "name"   VARCHAR(200),
    location VARCHAR(200),
    owner_id BIGINT,
    CONSTRAINT fk_garage_owner FOREIGN KEY (owner_id) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS parts (
                                     id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     "name"                     VARCHAR(200),
    "type"                     VARCHAR(50),
    hp_delta                   DOUBLE,
    torque_delta               DOUBLE,
    grip_delta                 DOUBLE,
    weight_delta               DOUBLE,
    suspension_stiffness_delta DOUBLE
    );

CREATE TABLE IF NOT EXISTS cars (
                                    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    make                      VARCHAR(100),
    model                     VARCHAR(100),
    "year"                    INT,
    base_power                DOUBLE,
    base_torque               DOUBLE,
    mass                      DOUBLE,
    grip_base                 DOUBLE,
    weight_distribution_front DOUBLE,
    garage_id                 BIGINT,
    CONSTRAINT fk_car_garage FOREIGN KEY (garage_id) REFERENCES garage(id)
    );

CREATE TABLE IF NOT EXISTS car_parts (
                                         car_id  BIGINT,
                                         part_id BIGINT,
                                         PRIMARY KEY (car_id, part_id),
    CONSTRAINT fk_cp_car  FOREIGN KEY (car_id)  REFERENCES cars(id),
    CONSTRAINT fk_cp_part FOREIGN KEY (part_id) REFERENCES parts(id)
    );

CREATE TABLE IF NOT EXISTS build_history (
                                             id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             car_id              BIGINT,
                                             owner_id            BIGINT,
                                             "timestamp"         TIMESTAMP,
                                             parts_snapshot_json CLOB,
                                             target_discipline   VARCHAR(50),
    CONSTRAINT fk_bh_car   FOREIGN KEY (car_id)   REFERENCES cars(id),
    CONSTRAINT fk_bh_owner FOREIGN KEY (owner_id) REFERENCES users(id)
    );
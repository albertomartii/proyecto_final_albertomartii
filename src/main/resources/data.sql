INSERT INTO users (username, email, role) VALUES ('alberto','alberto@example.com','USER');

INSERT INTO garage (name, location, owner_id) VALUES ('Garaje Central','Alicante',1);

INSERT INTO parts (name, type, hp_delta, torque_delta, grip_delta, weight_delta)
VALUES ('Turbo Stage 2','TURBO',60,80,0,10),
       ('Drift Tires Soft','TIRES',0,0,0.12,5);

INSERT INTO cars (make, model, year, base_power, base_torque, mass, grip_base, weight_distribution_front, garage_id)
VALUES ('Nissan','S13',1992,200,250,1200,1.0,0.48,1);

INSERT INTO car_parts (car_id, part_id) VALUES (1,1),(1,2);

INSERT INTO build_history (car_id, owner_id, timestamp, parts_snapshot_json, target_discipline)
VALUES (1,1, CURRENT_TIMESTAMP, '{"parts":["Turbo Stage 2","Drift Tires Soft"]}', 'DRIFT');

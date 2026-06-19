DELIMITER $$

CREATE TRIGGER hash_password_before_insert
    BEFORE INSERT ON users
    FOR EACH ROW
BEGIN
    SET NEW.password = SHA2(NEW.password, 256);
END$$

CREATE TRIGGER hash_password_before_update
    BEFORE UPDATE ON users
    FOR EACH ROW
BEGIN
    IF NEW.password <> OLD.password THEN
        SET NEW.password = SHA2(NEW.password, 256);
    END IF;
END$$

DELIMITER ;

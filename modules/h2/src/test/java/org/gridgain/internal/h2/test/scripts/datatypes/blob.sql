-- Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
-- and the EPL 1.0 (http://h2database.com/html/license.html).
-- Initial Developer: H2 Group
--

SELECT TYPE_NAME, PRECISION, PREFIX, SUFFIX, PARAMS, MINIMUM_SCALE, MAXIMUM_SCALE FROM INFORMATION_SCHEMA.TYPE_INFO
    WHERE TYPE_NAME = 'BLOB';
> TYPE_NAME PRECISION  PREFIX SUFFIX PARAMS MINIMUM_SCALE MAXIMUM_SCALE
> --------- ---------- ------ ------ ------ ------------- -------------
> BLOB      2147483647 X'     '      LENGTH 0             0
> rows: 1

CREATE TABLE TEST(B1 BLOB, B2 BINARY LARGE OBJECT, B3 TINYBLOB, B4 MEDIUMBLOB, B5 LONGBLOB, B6 IMAGE, B7 OID);
> ok

SELECT COLUMN_NAME, DATA_TYPE, TYPE_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'TEST' ORDER BY ORDINAL_POSITION;
> COLUMN_NAME DATA_TYPE TYPE_NAME COLUMN_TYPE
> ----------- --------- --------- -------------------
> B1          2004      BLOB      BLOB
> B2          2004      BLOB      BINARY LARGE OBJECT
> B3          2004      BLOB      TINYBLOB
> B4          2004      BLOB      MEDIUMBLOB
> B5          2004      BLOB      LONGBLOB
> B6          2004      BLOB      IMAGE
> B7          2004      BLOB      OID
> rows (ordered): 7

DROP TABLE TEST;
> ok

CREATE TABLE TEST(B0 BLOB(10), B1 BLOB(10K), B2 BLOB(10M), B3 BLOB(10G), B4 BLOB(10T), B5 BLOB(10P));
> ok

SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'TEST' ORDER BY ORDINAL_POSITION;
> COLUMN_NAME COLUMN_TYPE
> ----------- -----------------------
> B0          BLOB(10)
> B1          BLOB(10240)
> B2          BLOB(10485760)
> B3          BLOB(10737418240)
> B4          BLOB(10995116277760)
> B5          BLOB(11258999068426240)
> rows (ordered): 6

INSERT INTO TEST(B0) VALUES ('0102030405060708091011');
> exception VALUE_TOO_LONG_2

INSERT INTO TEST(B0) VALUES ('01020304050607080910');
> update count: 1

SELECT B0 FROM TEST;
>> 01020304050607080910

DROP TABLE TEST;
> ok

CREATE TABLE TEST(B BLOB(8192P));
> exception INVALID_VALUE_2

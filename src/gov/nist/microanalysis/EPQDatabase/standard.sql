--- These tables are for implementing standards and a standard block database

CREATE TABLE STDBLOCK (
    ID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    NAME VARCHAR(128),              --- The name of this standard block
    IMAGE_NAME CHAR(40),            --- Name of the block image
    NOTES VARCHAR(2048)             --- Generic note stucture
);

CREATE TABLE STDBLOCKPT (
    ID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    INSTRUMENT_KEY INT NOT NULL REFERENCES STDBLOCK(ID),
    X_COORD INT NOT NULL,
    Y_COORD INT NOT NULL
);

CREATE TABLE STANDARD {
    ID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    BLOCK INT NOT NULL REFERENCES STDBLOCK(ID),     --- In which block is it located?
    COMP INT NOT NULL REFERENCES ELEMENT_DATA(ID),  --- What is its composition?
    X_COORD INT,
    Y_COORD INT
);
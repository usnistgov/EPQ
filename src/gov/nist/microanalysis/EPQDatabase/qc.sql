--- QC database records

--- Defines a QC program for a specified detector based on measurements of a 
--- specified material at a certain beam energy and working distance.
CREATE TABLE QC_PROJECT (
	ID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	DETECTOR INT NOT NULL REFERENCES DETECTOR(ID),    --- the instrument/detector on which the spectrum was collected
	STANDARD VARCHAR(1024) NOT NULL REFERENCES STANDARD(NAME),
	BEAM_ENERGY REAL NOT NULL,
    NORMALIZATION INT DEFAULT 0,  --- How is the data to be normalized (current=0 or total counts=1)
	--- To encourage using the same acquisition conditions
	NOMINAL_WD REAL DEFAULT 0.0,
	NOMINAL_I REAL DEFAULT 0.0
);

--- Defines a set of QC measurement associated with a single measurement
CREATE TABLE QC_ENTRY (
	ID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	PROJECT INT NOT NULL REFERENCES QC_PROJECT(ID),
	CREATED TIMESTAMP,				--- When was this measurement recorded
	SPECTRUM INT NOT NULL REFERENCES SPECTRUM(ID) ON DELETE CASCADE,
	CULL INT DEFAULT 0  --- Should this data be removed from consideration in the normal reviews
);

--- Defines a single measured datum type.
CREATE TABLE QC_DATUM (
	ID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	NAME VARCHAR(40),
	PROJECT INT NOT NULL REFERENCES QC_PROJECT(ID),
	--- These columns record an optional nominal value and tolerance for this parameter.
	NOMINAL REAL DEFAULT NULL,
	TOLERANCE REAL DEFAULT NULL
);

CREATE UNIQUE INDEX QC_DATUM_INDEX ON QC_DATUM(NAME,PROJECT);
	
--- Records the measurement of a single QC_DATUM as part of a single QC measurement
CREATE TABLE QC_ITEM (
	ID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	ENTRY INT NOT NULL REFERENCES QC_ENTRY(ID),
	DATUM INT NOT NULL REFERENCES QC_DATUM(ID),
	QC_VALUE REAL NOT NULL,		--- The measured value
	QC_UNC REAL NOT NULL		   --- The uncertainty on the value
);

CREATE INDEX QC_ITEM_ENTRY_DATUM_IDX ON QC_ITEM(ENTRY,DATUM);


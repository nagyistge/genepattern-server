-- create index on the job_group table to speed up jdbc queries
CREATE INDEX IDX_JOB_GROUP_JOB_NO ON JOB_GROUP (JOB_NO);

INSERT INTO JOB_STATUS VALUES(5,'Dispatching');
INSERT INTO JOB_STATUS VALUES(0,'Waiting');

COMMIT;










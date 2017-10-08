/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.jobson.dao.jobs;

import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.JobId;
import com.github.jobson.dao.IdGenerator;
import com.github.jobson.jobs.states.PersistedJobRequest;
import com.github.jobson.jobs.states.ValidJobRequest;
import com.github.jobson.specs.JobSpec;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.github.jobson.Constants.JOB_DIR_JOB_DETAILS_FILENAME;
import static com.github.jobson.Helpers.*;
import static com.github.jobson.Helpers.readJSON;
import static com.github.jobson.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

public final class FilesystemJobsDAOTest extends JobsDAOTest {

    private static FilesystemJobsDAO createStandardFilesystemDAO() {
        try {
            final Path jobDir = createTmpDir(FilesystemJobsDAOTest.class);
            return createStandardFilesystemDAO(jobDir);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static FilesystemJobsDAO createStandardFilesystemDAO(Path jobDir) throws IOException {
        return new FilesystemJobsDAO(jobDir, createIdGenerator());
    }

    private static IdGenerator createIdGenerator() {
        return () -> generateRandomBase36String(10);
    }



    @Override
    protected JobDAO getInstance() {
        return createStandardFilesystemDAO();
    }



    @Test(expected = NullPointerException.class)
    public void testCtorThrowsIfPassedNulls() throws IOException {
        final Path jobDir = createTmpDir(FilesystemJobsDAOTest.class);
        final IdGenerator idGenerator = createIdGenerator();

        new FilesystemJobsDAO(jobDir, null);
        new FilesystemJobsDAO(null, idGenerator);
    }

    @Test(expected = FileNotFoundException.class)
    public void testCtorThrowsIfPassedANonExistentJobsDir() throws IOException {
        final Path invalidPath = Paths.get(genrateRandomAlphanumericString());
        createStandardFilesystemDAO(invalidPath);
    }



    @Test
    public void testPersistNewJobCreatesAJobDirNamedWithTheJobID() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(tryResolve(jobsDir, jobId)).isPresent();
    }

    private PersistedJobRequest persistValidRequest(Path jobsDir) throws IOException {
        return persistRequest(jobsDir, STANDARD_VALID_REQUEST);
    }

    private PersistedJobRequest persistRequest(Path jobsDir, ValidJobRequest jobRequest) throws IOException {
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        return dao.persist(jobRequest);
    }

    @Test
    public void testPersistNewJobJobDirectoryContainsAJobRequestJSONFile() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(tryResolve(jobsDir, jobId, JOB_DIR_JOB_DETAILS_FILENAME)).isPresent();
    }

    @Test
    public void testPersistNewJobJobDirectoryJobRequestJSONFileIsValidJSON() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        readJSON(tryResolve(jobsDir, jobId, JOB_DIR_JOB_DETAILS_FILENAME).get(), Object.class);
    }

    @Test
    public void testPersistNewJobDirJobRequestJSONFileIsJobDetails() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        readJSON(tryResolve(jobsDir, jobId, JOB_DIR_JOB_DETAILS_FILENAME).get(), JobDetails.class);
    }


    @Test
    public void testPersistNewJobJobDirectoryContainsTheJobsSchema() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(tryResolve(jobsDir, jobId, Constants.JOB_DIR_JOB_SPEC_FILENAME)).isPresent();
    }

    @Test
    public void testPersistNewJobJobDirectorySchemaFileIsValidJSON() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(Helpers.tryResolve(jobsDir, jobId, Constants.JOB_DIR_JOB_SPEC_FILENAME)).isPresent();
    }


    @Test
    public void testPersistNewJobJobDirectoryJSONParsesToAJobSchemaConfiguration() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        readJSON(tryResolve(jobsDir, jobId, Constants.JOB_DIR_JOB_SPEC_FILENAME).get(), JobSpec.class);
    }



    @Test
    public void testHasStdoutReturnsFalseIfTheStdoutFileWasDeleted() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO filesystemJobsDAO = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = filesystemJobsDAO.persist(STANDARD_VALID_REQUEST).getId();
        filesystemJobsDAO.appendStdout(jobId, TestHelpers.generateRandomByteObservable());
        final Path stdoutFile = jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_STDOUT_FILENAME);

        Files.delete(stdoutFile);

        assertThat(filesystemJobsDAO.hasStdout(jobId)).isFalse();
    }



    @Test
    public void testHasStderrReturnsFalseIfTheStderrFileWasDeleted() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        dao.appendStderr(jobId, TestHelpers.generateRandomByteObservable());
        final Path stderrFilePath = jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_STDERR_FILENAME);

        Files.delete(stderrFilePath);

        assertThat(dao.hasStderr(jobId)).isFalse();
    }

    @Test
    public void testGetSpecJobWasSubmittedAgainstReturnsOptionalEmptyIfJobDoesntExist() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        assertThat(dao.getSpecJobWasSubmittedAgainst(generateJobId())).isNotPresent();
    }

    @Test
    public void testGetSpecJobWasSubmittedAgainstReturnsSpecIfJobDoesExist() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final Optional<JobSpec> maybeJobSpec = dao.getSpecJobWasSubmittedAgainst(jobId);

        assertThat(maybeJobSpec).isPresent();
        assertThat(maybeJobSpec.get()).isEqualTo(STANDARD_VALID_REQUEST.getSpec());
    }
}
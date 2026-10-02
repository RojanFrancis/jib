/*
 * Copyright 2026 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.jib.registry;

import com.google.api.client.http.HttpStatusCodes;
import com.google.cloud.tools.jib.http.ResponseException;
import com.google.cloud.tools.jib.image.json.V22ManifestTemplate;
import com.google.cloud.tools.jib.json.JsonTemplateMapper;
import com.google.cloud.tools.jib.registry.json.ErrorEntryTemplate;
import com.google.cloud.tools.jib.registry.json.ErrorResponseTemplate;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests for {@link ManifestChecker}. */
@RunWith(MockitoJUnitRunner.class)
public class ManifestCheckerTest {

  private final RegistryEndpointRequestProperties fakeRegistryEndpointRequestProperties =
      new RegistryEndpointRequestProperties("someServerUrl", "someImageName");

  private ManifestChecker<V22ManifestTemplate> testManifestChecker;

  @Before
  public void setUpFakes() {
    testManifestChecker =
        new ManifestChecker<>(
            fakeRegistryEndpointRequestProperties, "someImageQualifier", V22ManifestTemplate.class);
  }

  @Test
  public void testHandleHttpResponseException_manifestUnknown() throws IOException {
    ResponseException mockResponseException = mockNotFoundResponse(ErrorCodes.MANIFEST_UNKNOWN);

    Assert.assertFalse(
        testManifestChecker.handleHttpResponseException(mockResponseException).isPresent());
  }

  @Test
  public void testHandleHttpResponseException_nameUnknown() throws IOException {
    ResponseException mockResponseException = mockNotFoundResponse(ErrorCodes.NAME_UNKNOWN);

    Assert.assertFalse(
        testManifestChecker.handleHttpResponseException(mockResponseException).isPresent());
  }

  @Test
  public void testHandleHttpResponseException_noContent() throws ResponseException {
    ResponseException mockResponseException = Mockito.mock(ResponseException.class);
    Mockito.when(mockResponseException.getStatusCode())
        .thenReturn(HttpStatusCodes.STATUS_CODE_NOT_FOUND);

    Assert.assertFalse(
        testManifestChecker.handleHttpResponseException(mockResponseException).isPresent());
  }

  @Test
  public void testHandleHttpResponseException_otherErrorCode() throws IOException {
    ResponseException mockResponseException = mockNotFoundResponse(ErrorCodes.BLOB_UNKNOWN);

    try {
      testManifestChecker.handleHttpResponseException(mockResponseException);
      Assert.fail("Error codes other than MANIFEST_UNKNOWN and NAME_UNKNOWN should not be handled");

    } catch (ResponseException ex) {
      Assert.assertEquals(mockResponseException, ex);
    }
  }

  @Test
  public void testHandleHttpResponseException_multipleErrors() throws IOException {
    ResponseException mockResponseException = Mockito.mock(ResponseException.class);
    Mockito.when(mockResponseException.getStatusCode())
        .thenReturn(HttpStatusCodes.STATUS_CODE_NOT_FOUND);

    ErrorResponseTemplate errorResponseTemplate =
        new ErrorResponseTemplate()
            .addError(new ErrorEntryTemplate(ErrorCodes.NAME_UNKNOWN.name(), "some message"))
            .addError(new ErrorEntryTemplate(ErrorCodes.MANIFEST_UNKNOWN.name(), "some message"));
    Mockito.when(mockResponseException.getContent())
        .thenReturn(JsonTemplateMapper.toUtf8String(errorResponseTemplate));

    try {
      testManifestChecker.handleHttpResponseException(mockResponseException);
      Assert.fail("Responses with more than one error should not be handled");

    } catch (ResponseException ex) {
      Assert.assertEquals(mockResponseException, ex);
    }
  }

  @Test
  public void testHandleHttpResponseException_invalidStatusCode() {
    ResponseException mockResponseException = Mockito.mock(ResponseException.class);
    Mockito.when(mockResponseException.getStatusCode()).thenReturn(-1);

    try {
      testManifestChecker.handleHttpResponseException(mockResponseException);
      Assert.fail("Non-404 status codes should not be handled");

    } catch (ResponseException ex) {
      Assert.assertEquals(mockResponseException, ex);
    }
  }

  /** Creates a mock 404 {@link ResponseException} carrying a single registry error code. */
  private static ResponseException mockNotFoundResponse(ErrorCodes errorCode) throws IOException {
    ResponseException mockResponseException = Mockito.mock(ResponseException.class);
    Mockito.when(mockResponseException.getStatusCode())
        .thenReturn(HttpStatusCodes.STATUS_CODE_NOT_FOUND);

    ErrorResponseTemplate errorResponseTemplate =
        new ErrorResponseTemplate()
            .addError(new ErrorEntryTemplate(errorCode.name(), "some message"));
    Mockito.when(mockResponseException.getContent())
        .thenReturn(JsonTemplateMapper.toUtf8String(errorResponseTemplate));
    return mockResponseException;
  }
}

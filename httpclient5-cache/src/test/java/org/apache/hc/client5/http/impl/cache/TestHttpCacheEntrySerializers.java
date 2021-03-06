/*
 * ====================================================================
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.hc.client5.http.impl.cache;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.cache.HttpCacheEntry;
import org.apache.hc.client5.http.cache.HttpCacheEntrySerializer;
import org.apache.hc.client5.http.cache.Resource;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;

public class TestHttpCacheEntrySerializers {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private HttpCacheEntrySerializer impl;

    @Before
    public void setUp() {
        impl = new DefaultHttpCacheEntrySerializer();
    }

    @Test
    public void canSerializeEntriesWithVariantMaps() throws Exception {
        readWriteVerify(makeCacheEntryWithVariantMap());
    }

    public void readWriteVerify(final HttpCacheEntry writeEntry) throws IOException {
        // write the entry
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        impl.writeTo(writeEntry, out);

        // read the entry
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final HttpCacheEntry readEntry = impl.readFrom(in);

        // compare
        assertTrue(areEqual(readEntry, writeEntry));
    }

    private HttpCacheEntry makeCacheEntryWithVariantMap() {
        final Header[] headers = new Header[5];
        for (int i = 0; i < headers.length; i++) {
            headers[i] = new BasicHeader("header" + i, "value" + i);
        }
        final String body = "Lorem ipsum dolor sit amet";

        final Map<String,String> variantMap = new HashMap<>();
        variantMap.put("test variant 1","true");
        variantMap.put("test variant 2","true");
        final HttpCacheEntry cacheEntry = new HttpCacheEntry(new Date(), new Date(),
                HttpStatus.SC_OK, headers,
                new HeapResource(Base64.decodeBase64(body.getBytes(UTF8))), variantMap);

        return cacheEntry;
    }

    private boolean areEqual(final HttpCacheEntry one, final HttpCacheEntry two) throws IOException {
        // dates are only stored with second precision, so scrub milliseconds
        if (!((one.getRequestDate().getTime() / 1000) == (two.getRequestDate()
                .getTime() / 1000))) {
            return false;
        }
        if (!((one.getResponseDate().getTime() / 1000) == (two
                .getResponseDate().getTime() / 1000))) {
            return false;
        }

        final byte[] onesByteArray = resourceToBytes(one.getResource());
        final byte[] twosByteArray = resourceToBytes(two.getResource());

        if (!Arrays.equals(onesByteArray,twosByteArray)) {
            return false;
        }

        final Header[] oneHeaders = one.getAllHeaders();
        final Header[] twoHeaders = two.getAllHeaders();
        if (!(oneHeaders.length == twoHeaders.length)) {
            return false;
        }
        for (int i = 0; i < oneHeaders.length; i++) {
            if (!oneHeaders[i].getName().equals(twoHeaders[i].getName())) {
                return false;
            }
            if (!oneHeaders[i].getValue().equals(twoHeaders[i].getValue())) {
                return false;
            }
        }

        return true;
    }

    private byte[] resourceToBytes(final Resource res) throws IOException {
        final InputStream inputStream = res.getInputStream();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int readBytes;
        final byte[] bytes = new byte[8096];
        while ((readBytes = inputStream.read(bytes)) > 0) {
            outputStream.write(bytes, 0, readBytes);
        }

        final byte[] byteData = outputStream.toByteArray();

        inputStream.close();
        outputStream.close();

        return byteData;
    }
}

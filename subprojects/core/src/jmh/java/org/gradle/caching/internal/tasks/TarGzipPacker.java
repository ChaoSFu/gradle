/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.internal.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TarGzipPacker extends TarPacker {
    @Override
    public void pack(List<DataSource> inputs, DataTarget output) throws IOException {
        super.pack(inputs, new DelegatingDataTarget(output) {
            @Override
            public OutputStream openOutput() throws IOException {
                return new GZIPOutputStream(super.openOutput());
            }
        });
    }

    @Override
    public void unpack(DataSource input, DataTargetFactory targetFactory) throws IOException {
        super.unpack(new DelegatingDataSource(input) {
            @Override
            public InputStream openInput() throws IOException {
                return new GZIPInputStream(super.openInput());
            }
        }, targetFactory);
    }
}

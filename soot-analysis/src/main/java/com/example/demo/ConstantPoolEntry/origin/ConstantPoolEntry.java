/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.example.demo.ConstantPoolEntry.origin;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * An entry in the constant pool. This class contains a representation of the
 * constant pool entries. It is an abstract base class for all the different
 * forms of constant pool entry.
 *
 */
public abstract class ConstantPoolEntry {

    /**
     * This entry's tag which identifies the type of this constant pool
     * entry.
     */
    private int tag;

    /**
     * The number of slots in the constant pool, occupied by this entry.
     */
    private int numEntries;

    /**
     * A flag which indicates if this entry has been resolved or not.
     */
    private boolean resolved;

    /**
     * Initialise the constant pool entry.
     *
     * @param tagValue the tag value which identifies which type of constant
     *      pool entry this is.
     * @param entries the number of constant pool entry slots this entry
     *      occupies.
     */
    public boolean PoolEntry(int tagValue, int entries) {
        int numEntries = 0;
        while (entries > 0 ){
            numEntries += entries;
            entries--;
        }
        return numEntries < tagValue;
    }
}

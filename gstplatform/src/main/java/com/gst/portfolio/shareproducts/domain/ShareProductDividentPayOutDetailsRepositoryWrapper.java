/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gst.portfolio.shareproducts.domain;

import com.gst.portfolio.shareproducts.exception.DividendNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ShareProductDividentPayOutDetailsRepositoryWrapper {

    private final ShareProductDividentPayOutDetailsRepository shareProductDividentPayOutDetailsRepository;

    @Autowired
    public ShareProductDividentPayOutDetailsRepositoryWrapper(
            final ShareProductDividentPayOutDetailsRepository shareProductDividentPayOutDetailsRepository) {
        this.shareProductDividentPayOutDetailsRepository = shareProductDividentPayOutDetailsRepository;
    }

    public ShareProductDividendPayOutDetails findOneWithNotFoundDetection(final Long dividendId) {
        ShareProductDividendPayOutDetails shareProductDividendPayOutDetails = this.shareProductDividentPayOutDetailsRepository
                .findOne(dividendId);
        if (shareProductDividendPayOutDetails == null) { throw new DividendNotFoundException(dividendId, "share"); }
        return shareProductDividendPayOutDetails;
    }

    public void save(final ShareProductDividendPayOutDetails shareProductDividendPayOutDetails) {
        this.shareProductDividentPayOutDetailsRepository.save(shareProductDividendPayOutDetails);
    }

    public void delete(final ShareProductDividendPayOutDetails shareProductDividendPayOutDetails) {
        this.shareProductDividentPayOutDetailsRepository.delete(shareProductDividendPayOutDetails);
    }

}

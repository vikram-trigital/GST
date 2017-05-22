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
package com.gst.portfolio.loanaccount.api;

import static com.gst.portfolio.loanproduct.service.LoanEnumerations.interestType;

import java.util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import com.gst.commands.domain.CommandWrapper;
import com.gst.commands.service.CommandWrapperBuilder;
import com.gst.commands.service.PortfolioCommandSourceWritePlatformService;
import com.gst.infrastructure.codes.data.CodeValueData;
import com.gst.infrastructure.codes.service.CodeValueReadPlatformService;
import com.gst.infrastructure.core.api.ApiParameterHelper;
import com.gst.infrastructure.core.api.ApiRequestParameterHelper;
import com.gst.infrastructure.core.api.JsonQuery;
import com.gst.infrastructure.core.data.CommandProcessingResult;
import com.gst.infrastructure.core.data.EnumOptionData;
import com.gst.infrastructure.core.exception.UnrecognizedQueryParamException;
import com.gst.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import com.gst.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import com.gst.infrastructure.core.serialization.FromJsonHelper;
import com.gst.infrastructure.core.service.Page;
import com.gst.infrastructure.core.service.SearchParameters;
import com.gst.infrastructure.dataqueries.data.DatatableData;
import com.gst.infrastructure.dataqueries.data.EntityTables;
import com.gst.infrastructure.dataqueries.data.StatusEnum;
import com.gst.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import com.gst.infrastructure.security.service.PlatformSecurityContext;
import com.gst.organisation.monetary.data.CurrencyData;
import com.gst.organisation.staff.data.StaffData;
import com.gst.portfolio.account.PortfolioAccountType;
import com.gst.portfolio.account.data.PortfolioAccountDTO;
import com.gst.portfolio.account.data.PortfolioAccountData;
import com.gst.portfolio.account.service.AccountAssociationsReadPlatformService;
import com.gst.portfolio.account.service.PortfolioAccountReadPlatformService;
import com.gst.portfolio.accountdetails.data.LoanAccountSummaryData;
import com.gst.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import com.gst.portfolio.calendar.data.CalendarData;
import com.gst.portfolio.calendar.domain.CalendarEntityType;
import com.gst.portfolio.calendar.service.CalendarReadPlatformService;
import com.gst.portfolio.charge.data.ChargeData;
import com.gst.portfolio.charge.domain.ChargeTimeType;
import com.gst.portfolio.charge.service.ChargeReadPlatformService;
import com.gst.portfolio.client.data.ClientData;
import com.gst.portfolio.collateral.data.CollateralData;
import com.gst.portfolio.collateral.service.CollateralReadPlatformService;
import com.gst.portfolio.floatingrates.data.InterestRatePeriodData;
import com.gst.portfolio.fund.data.FundData;
import com.gst.portfolio.fund.service.FundReadPlatformService;
import com.gst.portfolio.group.data.GroupGeneralData;
import com.gst.portfolio.group.service.GroupReadPlatformService;
import com.gst.portfolio.loanaccount.data.DisbursementData;
import com.gst.portfolio.loanaccount.data.LoanAccountData;
import com.gst.portfolio.loanaccount.data.LoanApprovalData;
import com.gst.portfolio.loanaccount.data.LoanChargeData;
import com.gst.portfolio.loanaccount.data.LoanTermVariationsData;
import com.gst.portfolio.loanaccount.data.LoanTransactionData;
import com.gst.portfolio.loanaccount.data.PaidInAdvanceData;
import com.gst.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import com.gst.portfolio.loanaccount.domain.LoanTermVariationType;
import com.gst.portfolio.loanaccount.exception.LoanTemplateTypeRequiredException;
import com.gst.portfolio.loanaccount.exception.NotSupportedLoanTemplateTypeException;
import com.gst.portfolio.loanaccount.guarantor.data.GuarantorData;
import com.gst.portfolio.loanaccount.guarantor.service.GuarantorReadPlatformService;
import com.gst.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import com.gst.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import com.gst.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import com.gst.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryReadPlatformService;
import com.gst.portfolio.loanaccount.service.LoanChargeReadPlatformService;
import com.gst.portfolio.loanaccount.service.LoanReadPlatformService;
import com.gst.portfolio.loanproduct.LoanProductConstants;
import com.gst.portfolio.loanproduct.data.LoanProductData;
import com.gst.portfolio.loanproduct.data.TransactionProcessingStrategyData;
import com.gst.portfolio.loanproduct.domain.InterestMethod;
import com.gst.portfolio.loanproduct.service.LoanDropdownReadPlatformService;
import com.gst.portfolio.loanproduct.service.LoanProductReadPlatformService;
import com.gst.portfolio.note.data.NoteData;
import com.gst.portfolio.note.domain.NoteType;
import com.gst.portfolio.note.service.NoteReadPlatformServiceImpl;
import com.gst.portfolio.savings.DepositAccountType;
import com.gst.portfolio.savings.domain.SavingsAccountStatusType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.gson.JsonElement;

@Path("/loans")
@Component
@Scope("singleton")
public class LoansApiResource {

    private final Set<String> LOAN_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "accountNo", "status", "externalId", "clientId",
            "group", "loanProductId", "loanProductName", "loanProductDescription", "isLoanProductLinkedToFloatingRate", "fundId",
            "fundName", "loanPurposeId", "loanPurposeName", "loanOfficerId", "loanOfficerName", "currency", "principal", "totalOverpaid",
            "inArrearsTolerance", "termFrequency", "termPeriodFrequencyType", "numberOfRepayments", "repaymentEvery",
            "interestRatePerPeriod", "annualInterestRate", "repaymentFrequencyType", "transactionProcessingStrategyId",
            "transactionProcessingStrategyName", "interestRateFrequencyType", "amortizationType", "interestType",
            "interestCalculationPeriodType", LoanProductConstants.allowPartialPeriodInterestCalcualtionParamName, "expectedFirstRepaymentOnDate",
            "graceOnPrincipalPayment", "recurringMoratoriumOnPrincipalPeriods", "graceOnInterestPayment", "graceOnInterestCharged", "interestChargedFromDate", "timeline",
            "totalFeeChargesAtDisbursement", "summary", "repaymentSchedule", "transactions", "charges", "collateral", "guarantors",
            "meeting", "productOptions", "amortizationTypeOptions", "interestTypeOptions", "interestCalculationPeriodTypeOptions",
            "repaymentFrequencyTypeOptions", "repaymentFrequencyNthDayTypeOptions", "repaymentFrequencyDaysOfWeekTypeOptions",
            "termFrequencyTypeOptions", "interestRateFrequencyTypeOptions", "fundOptions", "repaymentStrategyOptions", "chargeOptions",
            "loanOfficerOptions", "loanPurposeOptions", "loanCollateralOptions", "chargeTemplate", "calendarOptions",
            "syncDisbursementWithMeeting", "loanCounter", "loanProductCounter", "notes", "accountLinkingOptions", "linkedAccount",
            "interestRateDifferential", "isFloatingInterestRate", "interestRatesPeriods", LoanApiConstants.canUseForTopup,
            LoanApiConstants.isTopup, LoanApiConstants.loanIdToClose, LoanApiConstants.topupAmount, LoanApiConstants.clientActiveLoanOptions,
            LoanApiConstants.datatables));

    private final Set<String> LOAN_APPROVAL_DATA_PARAMETERS = new HashSet<>(Arrays.asList("approvalDate", "approvalAmount"));
    private final String resourceNameForPermissions = "LOAN";

    private final PlatformSecurityContext context;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final LoanDropdownReadPlatformService dropdownReadPlatformService;
    private final FundReadPlatformService fundReadPlatformService;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final LoanChargeReadPlatformService loanChargeReadPlatformService;
    private final CollateralReadPlatformService loanCollateralReadPlatformService;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;
    private final GuarantorReadPlatformService guarantorReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final DefaultToApiJsonSerializer<LoanAccountData> toApiJsonSerializer;
    private final DefaultToApiJsonSerializer<LoanApprovalData> loanApprovalDataToApiJsonSerializer;
    private final DefaultToApiJsonSerializer<LoanScheduleData> loanScheduleToApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final FromJsonHelper fromJsonHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final NoteReadPlatformServiceImpl noteReadPlatformService;
    private final PortfolioAccountReadPlatformService portfolioAccountReadPlatformService;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final LoanScheduleHistoryReadPlatformService loanScheduleHistoryReadPlatformService;
    private final AccountDetailsReadPlatformService accountDetailsReadPlatformService;
    private final EntityDatatableChecksReadService entityDatatableChecksReadService;

    @Autowired
    public LoansApiResource(final PlatformSecurityContext context, final LoanReadPlatformService loanReadPlatformService,
            final LoanProductReadPlatformService loanProductReadPlatformService,
            final LoanDropdownReadPlatformService dropdownReadPlatformService, final FundReadPlatformService fundReadPlatformService,
            final ChargeReadPlatformService chargeReadPlatformService, final LoanChargeReadPlatformService loanChargeReadPlatformService,
            final CollateralReadPlatformService loanCollateralReadPlatformService,
            final LoanScheduleCalculationPlatformService calculationPlatformService,
            final GuarantorReadPlatformService guarantorReadPlatformService,
            final CodeValueReadPlatformService codeValueReadPlatformService, final GroupReadPlatformService groupReadPlatformService,
            final DefaultToApiJsonSerializer<LoanAccountData> toApiJsonSerializer,
            final DefaultToApiJsonSerializer<LoanApprovalData> loanApprovalDataToApiJsonSerializer,
            final DefaultToApiJsonSerializer<LoanScheduleData> loanScheduleToApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper, final FromJsonHelper fromJsonHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final CalendarReadPlatformService calendarReadPlatformService, final NoteReadPlatformServiceImpl noteReadPlatformService,
            final PortfolioAccountReadPlatformService portfolioAccountReadPlatformServiceImpl,
            final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService,
            final LoanScheduleHistoryReadPlatformService loanScheduleHistoryReadPlatformService,
            final AccountDetailsReadPlatformService accountDetailsReadPlatformService,
            final EntityDatatableChecksReadService entityDatatableChecksReadService) {
        this.context = context;
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
        this.dropdownReadPlatformService = dropdownReadPlatformService;
        this.fundReadPlatformService = fundReadPlatformService;
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.loanChargeReadPlatformService = loanChargeReadPlatformService;
        this.loanCollateralReadPlatformService = loanCollateralReadPlatformService;
        this.calculationPlatformService = calculationPlatformService;
        this.guarantorReadPlatformService = guarantorReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.groupReadPlatformService = groupReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.loanApprovalDataToApiJsonSerializer = loanApprovalDataToApiJsonSerializer;
        this.loanScheduleToApiJsonSerializer = loanScheduleToApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.fromJsonHelper = fromJsonHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.calendarReadPlatformService = calendarReadPlatformService;
        this.noteReadPlatformService = noteReadPlatformService;
        this.portfolioAccountReadPlatformService = portfolioAccountReadPlatformServiceImpl;
        this.accountAssociationsReadPlatformService = accountAssociationsReadPlatformService;
        this.loanScheduleHistoryReadPlatformService = loanScheduleHistoryReadPlatformService;
        this.accountDetailsReadPlatformService = accountDetailsReadPlatformService;
        this.entityDatatableChecksReadService = entityDatatableChecksReadService;
    }

    /*
     * This template API is used for loan approval, ideally this should be
     * invoked on loan that are pending for approval. But system does not
     * validate the status of the loan, it returns the template irrespective of
     * loan status
     */

    @GET
    @Path("{loanId}/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveApprovalTemplate(@PathParam("loanId") final Long loanId, @QueryParam("templateType") final String templateType,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        LoanApprovalData loanApprovalTemplate = null;

        if (templateType == null) {
            final String errorMsg = "Loan template type must be provided";
            throw new LoanTemplateTypeRequiredException(errorMsg);
        } else if (templateType.equals("approval")) {
            loanApprovalTemplate = this.loanReadPlatformService.retrieveApprovalTemplate(loanId);
        }

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.loanApprovalDataToApiJsonSerializer.serialize(settings, loanApprovalTemplate, this.LOAN_APPROVAL_DATA_PARAMETERS);

    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String template(@QueryParam("clientId") final Long clientId, @QueryParam("groupId") final Long groupId,
            @QueryParam("productId") final Long productId, @QueryParam("templateType") final String templateType,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @DefaultValue("false") @QueryParam("activeOnly") final boolean onlyActive, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        // template
        final Collection<LoanProductData> productOptions = this.loanProductReadPlatformService.retrieveAllLoanProductsForLookup(onlyActive);

        // options
        Collection<StaffData> allowedLoanOfficers = null;
        Collection<CodeValueData> loanCollateralOptions = null;
        Collection<CalendarData> calendarOptions = null;
        LoanAccountData newLoanAccount = null;
        Long officeId = null;

        if (productId != null) {
            newLoanAccount = this.loanReadPlatformService.retrieveLoanProductDetailsTemplate(productId, clientId, groupId);
        }

        if (templateType == null) {
            final String errorMsg = "Loan template type must be provided";
            throw new LoanTemplateTypeRequiredException(errorMsg);
        } else if (templateType.equals("collateral")) {
            loanCollateralOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanCollateral");
            newLoanAccount = LoanAccountData.collateralTemplate(loanCollateralOptions);
        } else {
            // for JLG loan both client and group details are required
            if (templateType.equals("individual") || templateType.equals("jlg")) {

                if(clientId == null) {
                    newLoanAccount = newLoanAccount == null ? LoanAccountData.emptyTemplate() : newLoanAccount;
                } else {
                    final LoanAccountData loanAccountClientDetails = this.loanReadPlatformService.retrieveClientDetailsTemplate(clientId);

                    officeId = loanAccountClientDetails.officeId();
                    newLoanAccount = newLoanAccount == null ? loanAccountClientDetails : LoanAccountData.populateClientDefaults(newLoanAccount,
                            loanAccountClientDetails);
                }

                // if it's JLG loan add group details
                if (templateType.equals("jlg")) {
                    final GroupGeneralData group = this.groupReadPlatformService.retrieveOne(groupId);
                    newLoanAccount = LoanAccountData.associateGroup(newLoanAccount, group);
                    calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                }

            } else if (templateType.equals("group")) {

                final LoanAccountData loanAccountGroupData = this.loanReadPlatformService.retrieveGroupDetailsTemplate(groupId);
                officeId = loanAccountGroupData.groupOfficeId();
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                newLoanAccount = newLoanAccount == null ? loanAccountGroupData : LoanAccountData.populateGroupDefaults(newLoanAccount,
                        loanAccountGroupData);

            } else if (templateType.equals("jlgbulk")) {
                // get group details along with members in that group
                final LoanAccountData loanAccountGroupData = this.loanReadPlatformService.retrieveGroupAndMembersDetailsTemplate(groupId);
                officeId = loanAccountGroupData.groupOfficeId();
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(groupId);
                newLoanAccount = newLoanAccount == null ? loanAccountGroupData : LoanAccountData.populateGroupDefaults(newLoanAccount,
                        loanAccountGroupData);
                if (productId != null) {
                    Map<Long, Integer> memberLoanCycle = new HashMap<>();
                    Collection<ClientData> members = loanAccountGroupData.groupData().clientMembers();
                    for (ClientData clientData : members) {
                        Integer loanCounter = this.loanReadPlatformService.retriveLoanCounter(clientData.id(), productId);
                        memberLoanCycle.put(clientData.id(), loanCounter);
                    }
                    newLoanAccount = LoanAccountData.associateMemberVariations(newLoanAccount, memberLoanCycle);
                }

            } else {
                final String errorMsg = "Loan template type '" + templateType + "' is not supported";
                throw new NotSupportedLoanTemplateTypeException(errorMsg, templateType);
            }

            allowedLoanOfficers = this.loanReadPlatformService.retrieveAllowedLoanOfficers(officeId, staffInSelectedOfficeOnly);

            Collection<PortfolioAccountData> accountLinkingOptions = null;
            if (clientId != null) {
                final CurrencyData currencyData = newLoanAccount.currency();
                String currencyCode = null;
                if (currencyData != null) {
                    currencyCode = currencyData.code();
                }
                final long[] accountStatus = { SavingsAccountStatusType.ACTIVE.getValue() };
                PortfolioAccountDTO portfolioAccountDTO = new PortfolioAccountDTO(PortfolioAccountType.SAVINGS.getValue(), clientId,
                        currencyCode, accountStatus, DepositAccountType.SAVINGS_DEPOSIT.getValue());
                accountLinkingOptions = this.portfolioAccountReadPlatformService.retrieveAllForLookup(portfolioAccountDTO);

            }

            // add product options, allowed loan officers and calendar options
            // (calendar options will be null in individual loan)
            newLoanAccount = LoanAccountData.associationsAndTemplate(newLoanAccount, productOptions, allowedLoanOfficers, calendarOptions,
                    accountLinkingOptions);
        }
        final List<DatatableData> datatableTemplates = this.entityDatatableChecksReadService
                .retrieveTemplates(StatusEnum.CREATE.getCode().longValue(), EntityTables.LOAN.getName(), productId);
        newLoanAccount.setDatatables(datatableTemplates);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, newLoanAccount, this.LOAN_DATA_PARAMETERS);
    }

    @GET
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveLoan(@PathParam("loanId") final Long loanId,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @Context final UriInfo uriInfo) {
        long start = System.currentTimeMillis() ;
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loanId);
        if (loanBasicDetails.isInterestRecalculationEnabled()) {
            Collection<CalendarData> interestRecalculationCalendarDatas = this.calendarReadPlatformService
                    .retrieveCalendarsByEntity(loanBasicDetails.getInterestRecalculationDetailId(),
                            CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL.getValue(), null);
            CalendarData calendarData = null;
            if (!CollectionUtils.isEmpty(interestRecalculationCalendarDatas)) {
                calendarData = interestRecalculationCalendarDatas.iterator().next();
            }

            Collection<CalendarData> interestRecalculationCompoundingCalendarDatas = this.calendarReadPlatformService
                    .retrieveCalendarsByEntity(loanBasicDetails.getInterestRecalculationDetailId(),
                            CalendarEntityType.LOAN_RECALCULATION_COMPOUNDING_DETAIL.getValue(), null);
            CalendarData compoundingCalendarData = null;
            if (!CollectionUtils.isEmpty(interestRecalculationCompoundingCalendarDatas)) {
                compoundingCalendarData = interestRecalculationCompoundingCalendarDatas.iterator().next();
            }
            loanBasicDetails = LoanAccountData.withInterestRecalculationCalendarData(loanBasicDetails, calendarData,
                    compoundingCalendarData);
        }
        if (loanBasicDetails.isMonthlyRepaymentFrequencyType()) {
        	Collection<CalendarData> loanCalendarDatas = this.calendarReadPlatformService
                    .retrieveCalendarsByEntity(loanId,
                            CalendarEntityType.LOANS.getValue(), null);
            CalendarData calendarData = null;
            if (!CollectionUtils.isEmpty(loanCalendarDatas)) {
                calendarData = loanCalendarDatas.iterator().next();
            }
            if(calendarData != null)
            	loanBasicDetails = LoanAccountData.withLoanCalendarData(loanBasicDetails, calendarData);
        }
        Collection<InterestRatePeriodData> interestRatesPeriods = this.loanReadPlatformService.retrieveLoanInterestRatePeriodData(loanBasicDetails);
        Collection<LoanTransactionData> loanRepayments = null;
        LoanScheduleData repaymentSchedule = null;
        Collection<LoanChargeData> charges = null;
        Collection<GuarantorData> guarantors = null;
        Collection<CollateralData> collateral = null;
        CalendarData meeting = null;
        Collection<NoteData> notes = null;
        PortfolioAccountData linkedAccount = null;
        Collection<DisbursementData> disbursementData = null;
        Collection<LoanTermVariationsData> emiAmountVariations = null;

        final Set<String> mandatoryResponseParameters = new HashSet<>();
        final Set<String> associationParameters = ApiParameterHelper.extractAssociationsForResponseIfProvided(uriInfo.getQueryParameters());
        if (!associationParameters.isEmpty()) {

            if (associationParameters.contains("all")) {
                associationParameters.addAll(Arrays.asList("repaymentSchedule", "futureSchedule", "originalSchedule", "transactions",
                        "charges", "guarantors", "collateral", "notes", "linkedAccount", "multiDisburseDetails"));
            }

            ApiParameterHelper.excludeAssociationsForResponseIfProvided(uriInfo.getQueryParameters(), associationParameters);

            if (associationParameters.contains("guarantors")) {
                mandatoryResponseParameters.add("guarantors");
                guarantors = this.guarantorReadPlatformService.retrieveGuarantorsForLoan(loanId);
                if (CollectionUtils.isEmpty(guarantors)) {
                    guarantors = null;
                }
            }

            if (associationParameters.contains("transactions")) {
                mandatoryResponseParameters.add("transactions");
                final Collection<LoanTransactionData> currentLoanRepayments = this.loanReadPlatformService.retrieveLoanTransactions(loanId);
                if (!CollectionUtils.isEmpty(currentLoanRepayments)) {
                    loanRepayments = currentLoanRepayments;
                }
            }

            if (associationParameters.contains("multiDisburseDetails") || associationParameters.contains("repaymentSchedule")) {
                mandatoryResponseParameters.add("multiDisburseDetails");
                disbursementData = this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId);
            }

            if (associationParameters.contains("emiAmountVariations") || associationParameters.contains("repaymentSchedule")) {
                mandatoryResponseParameters.add("emiAmountVariations");
                emiAmountVariations = this.loanReadPlatformService.retrieveLoanTermVariations(loanId,
                        LoanTermVariationType.EMI_AMOUNT.getValue());
            }

            if (associationParameters.contains("repaymentSchedule")) {
                mandatoryResponseParameters.add("repaymentSchedule");
                final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData = loanBasicDetails.repaymentScheduleRelatedData();
                repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId, repaymentScheduleRelatedData,
                        disbursementData, loanBasicDetails.isInterestRecalculationEnabled(), loanBasicDetails.getTotalPaidFeeCharges());

                if (associationParameters.contains("futureSchedule") && loanBasicDetails.isInterestRecalculationEnabled()) {
                    mandatoryResponseParameters.add("futureSchedule");
                    this.calculationPlatformService.updateFutureSchedule(repaymentSchedule, loanId);
                }

                if (associationParameters.contains("originalSchedule") && loanBasicDetails.isInterestRecalculationEnabled()
                        && loanBasicDetails.isActive()) {
                    mandatoryResponseParameters.add("originalSchedule");
                    LoanScheduleData loanScheduleData = this.loanScheduleHistoryReadPlatformService.retrieveRepaymentArchiveSchedule(
                            loanId, repaymentScheduleRelatedData, disbursementData);
                    loanBasicDetails = LoanAccountData.withOriginalSchedule(loanBasicDetails, loanScheduleData);
                }
            }

            if (associationParameters.contains("charges")) {
                mandatoryResponseParameters.add("charges");
                charges = this.loanChargeReadPlatformService.retrieveLoanCharges(loanId);
                if (CollectionUtils.isEmpty(charges)) {
                    charges = null;
                }
            }

            if (associationParameters.contains("collateral")) {
                mandatoryResponseParameters.add("collateral");
                collateral = this.loanCollateralReadPlatformService.retrieveCollaterals(loanId);
                if (CollectionUtils.isEmpty(collateral)) {
                    collateral = null;
                }
            }

            if (associationParameters.contains("meeting")) {
                mandatoryResponseParameters.add("meeting");
                meeting = this.calendarReadPlatformService.retrieveLoanCalendar(loanId);
            }

            if (associationParameters.contains("notes")) {
                mandatoryResponseParameters.add("notes");
                notes = this.noteReadPlatformService.retrieveNotesByResource(loanId, NoteType.LOAN.getValue());
                if (CollectionUtils.isEmpty(notes)) {
                    notes = null;
                }
            }

            if (associationParameters.contains("linkedAccount")) {
                mandatoryResponseParameters.add("linkedAccount");
                linkedAccount = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
            }

        }

        Collection<LoanProductData> productOptions = null;
        LoanProductData product = null;
        Collection<EnumOptionData> loanTermFrequencyTypeOptions = null;
        Collection<EnumOptionData> repaymentFrequencyTypeOptions = null;
        Collection<EnumOptionData> repaymentFrequencyNthDayTypeOptions = null;
        Collection<EnumOptionData> repaymentFrequencyDayOfWeekTypeOptions = null;
        Collection<TransactionProcessingStrategyData> repaymentStrategyOptions = null;
        Collection<EnumOptionData> interestRateFrequencyTypeOptions = null;
        Collection<EnumOptionData> amortizationTypeOptions = null;
        Collection<EnumOptionData> interestTypeOptions = null;
        Collection<EnumOptionData> interestCalculationPeriodTypeOptions = null;
        Collection<FundData> fundOptions = null;
        Collection<StaffData> allowedLoanOfficers = null;
        Collection<ChargeData> chargeOptions = null;
        ChargeData chargeTemplate = null;
        Collection<CodeValueData> loanPurposeOptions = null;
        Collection<CodeValueData> loanCollateralOptions = null;
        Collection<CalendarData> calendarOptions = null;
        Collection<PortfolioAccountData> accountLinkingOptions = null;
        PaidInAdvanceData paidInAdvanceTemplate = null;
        Collection<LoanAccountSummaryData> clientActiveLoanOptions = null;

        final boolean template = ApiParameterHelper.template(uriInfo.getQueryParameters());
        if (template) {
            productOptions = this.loanProductReadPlatformService.retrieveAllLoanProductsForLookup();
            product = this.loanProductReadPlatformService.retrieveLoanProduct(loanBasicDetails.loanProductId());
            loanBasicDetails.setProduct(product);
            loanTermFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveLoanTermFrequencyTypeOptions();
            repaymentFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyTypeOptions();
            repaymentFrequencyNthDayTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyOptionsForNthDayOfMonth();
            repaymentFrequencyDayOfWeekTypeOptions = this.dropdownReadPlatformService.retrieveRepaymentFrequencyOptionsForDaysOfWeek();
            interestRateFrequencyTypeOptions = this.dropdownReadPlatformService.retrieveInterestRateFrequencyTypeOptions();

            amortizationTypeOptions = this.dropdownReadPlatformService.retrieveLoanAmortizationTypeOptions();
            if (product.isLinkedToFloatingInterestRates()) {
                interestTypeOptions = Arrays.asList(interestType(InterestMethod.DECLINING_BALANCE));
            } else {
                interestTypeOptions = this.dropdownReadPlatformService.retrieveLoanInterestTypeOptions();
            }
            interestCalculationPeriodTypeOptions = this.dropdownReadPlatformService.retrieveLoanInterestRateCalculatedInPeriodOptions();

            fundOptions = this.fundReadPlatformService.retrieveAllFunds();
            repaymentStrategyOptions = this.dropdownReadPlatformService.retreiveTransactionProcessingStrategies();
            if (product.getMultiDisburseLoan()) {
                chargeOptions = this.chargeReadPlatformService.retrieveLoanAccountApplicableCharges(loanId,
                        new ChargeTimeType[] { ChargeTimeType.OVERDUE_INSTALLMENT });
            } else {
                chargeOptions = this.chargeReadPlatformService.retrieveLoanAccountApplicableCharges(loanId, new ChargeTimeType[] {
                        ChargeTimeType.OVERDUE_INSTALLMENT, ChargeTimeType.TRANCHE_DISBURSEMENT });
            }
            chargeTemplate = this.loanChargeReadPlatformService.retrieveLoanChargeTemplate();

            allowedLoanOfficers = this.loanReadPlatformService.retrieveAllowedLoanOfficers(loanBasicDetails.officeId(),
                    staffInSelectedOfficeOnly);

            loanPurposeOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanPurpose");
            loanCollateralOptions = this.codeValueReadPlatformService.retrieveCodeValuesByCode("LoanCollateral");
            final CurrencyData currencyData = loanBasicDetails.currency();
            String currencyCode = null;
            if (currencyData != null) {
                currencyCode = currencyData.code();
            }
            final long[] accountStatus = { SavingsAccountStatusType.ACTIVE.getValue() };
            PortfolioAccountDTO portfolioAccountDTO = new PortfolioAccountDTO(PortfolioAccountType.SAVINGS.getValue(),
                    loanBasicDetails.clientId(), currencyCode, accountStatus, DepositAccountType.SAVINGS_DEPOSIT.getValue());
            accountLinkingOptions = this.portfolioAccountReadPlatformService.retrieveAllForLookup(portfolioAccountDTO);

            if (!associationParameters.contains("linkedAccount")) {
                mandatoryResponseParameters.add("linkedAccount");
                linkedAccount = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
            }
            if (loanBasicDetails.groupId() != null) {
                calendarOptions = this.loanReadPlatformService.retrieveCalendars(loanBasicDetails.groupId());
            }

            if(loanBasicDetails.product().canUseForTopup() && loanBasicDetails.clientId() != null){
                clientActiveLoanOptions = this.accountDetailsReadPlatformService.retrieveClientActiveLoanAccountSummary(loanBasicDetails.clientId());
            }


        }

        Collection<ChargeData> overdueCharges = this.chargeReadPlatformService.retrieveLoanProductCharges(loanBasicDetails.loanProductId(),
                ChargeTimeType.OVERDUE_INSTALLMENT);

        paidInAdvanceTemplate = this.loanReadPlatformService.retrieveTotalPaidInAdvance(loanId);

        final LoanAccountData loanAccount = LoanAccountData.associationsAndTemplate(loanBasicDetails, repaymentSchedule, loanRepayments,
                charges, collateral, guarantors, meeting, productOptions, loanTermFrequencyTypeOptions, repaymentFrequencyTypeOptions,
                repaymentFrequencyNthDayTypeOptions, repaymentFrequencyDayOfWeekTypeOptions, repaymentStrategyOptions, 
                interestRateFrequencyTypeOptions, amortizationTypeOptions, interestTypeOptions, interestCalculationPeriodTypeOptions, 
                fundOptions, chargeOptions, chargeTemplate, allowedLoanOfficers, loanPurposeOptions, loanCollateralOptions, 
                calendarOptions, notes, accountLinkingOptions, linkedAccount, disbursementData, emiAmountVariations,
                overdueCharges, paidInAdvanceTemplate, interestRatesPeriods, clientActiveLoanOptions);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters(),
                mandatoryResponseParameters);
        long end = System.currentTimeMillis() ;
        System.out.println("LoansApiResource.retrieveLoan() Time took: "+(end-start));
        start = System.currentTimeMillis() ;
        String toReturn = this.toApiJsonSerializer.serialize(settings, loanAccount, this.LOAN_DATA_PARAMETERS);
        end = System.currentTimeMillis() ;
        System.out.println("LoansApiResource.retrieveLoan() Time took to Serialize: "+(end-start));
        return toReturn ;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo,
            @QueryParam("sqlSearch") final String sqlSearch,
            @QueryParam("externalId") final String externalId,
            // @QueryParam("underHierarchy") final String hierarchy,
            @QueryParam("offset") final Integer offset, @QueryParam("limit") final Integer limit,
            @QueryParam("orderBy") final String orderBy, @QueryParam("sortOrder") final String sortOrder,
            @QueryParam("accountNo") final String accountNo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        final SearchParameters searchParameters = SearchParameters.forLoans(sqlSearch, externalId, offset, limit, orderBy, sortOrder,
                accountNo);

        final Page<LoanAccountData> loanBasicDetails = this.loanReadPlatformService.retrieveAll(searchParameters);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, loanBasicDetails, this.LOAN_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String calculateLoanScheduleOrSubmitLoanApplication(@QueryParam("command") final String commandParam,
            @Context final UriInfo uriInfo, final String apiRequestBodyAsJson) {

        if (is(commandParam, "calculateLoanSchedule")) {

            final JsonElement parsedQuery = this.fromJsonHelper.parse(apiRequestBodyAsJson);
            final JsonQuery query = JsonQuery.from(apiRequestBodyAsJson, parsedQuery, this.fromJsonHelper);

            final LoanScheduleModel loanSchedule = this.calculationPlatformService.calculateLoanSchedule(query, true);

            final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
            return this.loanScheduleToApiJsonSerializer.serialize(settings, loanSchedule.toData(), new HashSet<String>());
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createLoanApplication().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String modifyLoanApplication(@PathParam("loanId") final Long loanId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateLoanApplication(loanId).withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteLoanApplication(@PathParam("loanId") final Long loanId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteLoanApplication(loanId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String stateTransitions(@PathParam("loanId") final Long loanId, @QueryParam("command") final String commandParam,
            final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandProcessingResult result = null;

        if (is(commandParam, "reject")) {
            final CommandWrapper commandRequest = builder.rejectLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "withdrawnByApplicant")) {
            final CommandWrapper commandRequest = builder.withdrawLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "approve")) {
            final CommandWrapper commandRequest = builder.approveLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "disburse")) {
            final CommandWrapper commandRequest = builder.disburseLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "disburseToSavings")) {
            final CommandWrapper commandRequest = builder.disburseLoanToSavingsApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (is(commandParam, "undoapproval")) {
            final CommandWrapper commandRequest = builder.undoLoanApplicationApproval(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "undodisbursal")) {
            final CommandWrapper commandRequest = builder.undoLoanApplicationDisbursal(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }else if (is(commandParam, "undolastdisbursal")) {
            final CommandWrapper commandRequest = builder.undoLastDisbursalLoanApplication(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (is(commandParam, "assignloanofficer")) {
            final CommandWrapper commandRequest = builder.assignLoanOfficer(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "unassignloanofficer")) {
            final CommandWrapper commandRequest = builder.unassignLoanOfficer(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "recoverGuarantees")) {
            final CommandWrapper commandRequest = new CommandWrapperBuilder().recoverFromGuarantor(loanId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) { throw new UnrecognizedQueryParamException("command", commandParam); }

        return this.toApiJsonSerializer.serialize(result);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
package org.sunbird.learner.actors.certificate.service;

import java.text.MessageFormat;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actor.base.BaseActor;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.kafka.client.InstructionEventGenerator;
import org.sunbird.learner.constants.CourseJsonKey;
import org.sunbird.learner.constants.InstructionEvent;
import org.sunbird.learner.util.CourseBatchUtil;
import org.sunbird.learner.util.Util;

public class CertificateActor extends BaseActor {
  

  private static enum ResponseMessage {
    SUBMITTED("Certificates issue action for Course Batch Id {0} submitted Successfully!"),
    FAILED("Certificates issue action for Course Batch Id {0} Failed!"),
    PIAA_SUBMITTED("Certificates will be issued for Course Batch Id {0} after evaluation ");
    private String value;

    private ResponseMessage(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  };

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER, this.getClass().getName());

    String requestedOperation = request.getOperation();
    switch (requestedOperation) {
      case "issueCertificate":
        issueCertificate(request);
        break;
      case "issueCertificateForPIAA":
        issueCertificateForPIAA(request);
        break;
      default:
        onReceiveUnsupportedOperation(request.getOperation());
        break;
    }
  }

  private void issueCertificate(Request request) {
    logger.info(request.getRequestContext(), "issueCertificate request=" + request.getRequest());
    final String batchId = (String) request.getRequest().get(JsonKey.BATCH_ID);
    final String courseId = (String) request.getRequest().get(JsonKey.COURSE_ID);
    List<String> userIds = (List<String>) request.getRequest().get(JsonKey.USER_IDs);
    final boolean reIssue = isReissue(request.getContext().get(CourseJsonKey.REISSUE));
    Map<String, Object> courseBatchResponse =
        CourseBatchUtil.validateCourseBatch(request.getRequestContext(), courseId, batchId);
    if (null == courseBatchResponse.get("cert_templates")) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.CLIENT_ERROR, "No certificate templates associated with " + batchId);
    }
    Map<String, Object> courseDetailsResponse =
            CourseBatchUtil.getCourseDetails(request.getRequestContext(), courseId);
    Response response = new Response();
    Map<String, Object> resultData = new HashMap<>();
    if(courseDetailsResponse.get(JsonKey.PRIMARYCATEGORY).equals("PIAA Assessment")){
      //Do Nothing
    }else{
      resultData.put(JsonKey.STATUS, MessageFormat.format(ResponseMessage.SUBMITTED.getValue(), batchId));
      resultData.put(JsonKey.BATCH_ID, batchId);
      resultData.put(JsonKey.COURSE_ID, courseId);
      resultData.put(JsonKey.COLLECTION_ID, courseId);
      response.put(JsonKey.RESULT, resultData);
      try {
        pushInstructionEvent(batchId, courseId, userIds, reIssue);
      } catch (Exception e) {
        logger.error(request.getRequestContext(), "issueCertificate pushInstructionEvent error for courseId="
                + courseId + ", batchId=" + batchId, e);
        resultData.put(
                JsonKey.STATUS, MessageFormat.format(ResponseMessage.FAILED.getValue(), batchId));
      }
    }
    sender().tell(response, self());
  }

  private void issueCertificateForPIAA(Request request) {
    logger.info(request.getRequestContext(), "issueCertificateForPIAA request=" + request.getRequest());
    final String batchId = (String) request.getRequest().get(JsonKey.BATCH_ID);
    final String courseId = (String) request.getRequest().get(JsonKey.COURSE_ID);
    List<String> userIds = (List<String>) request.getRequest().get(JsonKey.USER_IDs);
    final boolean reIssue = isReissue(request.getContext().get(CourseJsonKey.REISSUE));
    Map<String, Object> courseBatchResponse =
            CourseBatchUtil.validateCourseBatch(request.getRequestContext(), courseId, batchId);
    if (null == courseBatchResponse.get("cert_templates")) {
      ProjectCommonException.throwClientErrorException(
              ResponseCode.CLIENT_ERROR, "No certificate templates associated with " + batchId);
    }
    Map<String, Object> courseDetailsResponse =
            CourseBatchUtil.getCourseDetails(request.getRequestContext(), courseId);
    Response response = new Response();
    Map<String, Object> resultData = new HashMap<>();
    if(courseDetailsResponse.get(JsonKey.PRIMARYCATEGORY).equals("PIAA Assessment")){
      resultData.put(JsonKey.STATUS, MessageFormat.format(ResponseMessage.PIAA_SUBMITTED.getValue(), batchId));
      resultData.put(JsonKey.BATCH_ID, batchId);
      resultData.put(JsonKey.COURSE_ID, courseId);
      resultData.put(JsonKey.COLLECTION_ID, courseId);
      response.put(JsonKey.RESULT, resultData);
      try {
        pushInstructionEvent(batchId, courseId, userIds, reIssue);
      } catch (Exception e) {
        logger.error(request.getRequestContext(), "issueCertificate pushInstructionEvent error for courseId="
                + courseId + ", batchId=" + batchId, e);
        resultData.put(
                JsonKey.STATUS, MessageFormat.format(ResponseMessage.FAILED.getValue(), batchId));
      }
    }
    sender().tell(response, self());
  }
  private boolean isReissue(Object queryString) {
    if (queryString != null) {
      if (queryString instanceof String[]) {
        String query = Arrays.stream((String[]) queryString).findFirst().orElse(null);
        return Boolean.parseBoolean(query);
      } else if (queryString instanceof String) {
        return Boolean.parseBoolean((String) queryString);
      }
    }
    return false;
  }

  /**
   * Construct the instruction event data and push the event data as BEInstructionEvent.
   *
   * @param batchId
   * @param courseId
   * @throws Exception
   */
  private void pushInstructionEvent(
      String batchId, String courseId, List<String> userIds, boolean reIssue) throws Exception {
    Map<String, Object> data = new HashMap<>();

    data.put(
        CourseJsonKey.ACTOR,
        new HashMap<String, Object>() {
          {
            put(JsonKey.ID, InstructionEvent.CERTIFICATE_GENERATOR.getActorId());
            put(JsonKey.TYPE, InstructionEvent.CERTIFICATE_GENERATOR.getActorType());
          }
        });


    String id = OneWayHashing.encryptVal(batchId + CourseJsonKey.UNDERSCORE + courseId);
    data.put(
        CourseJsonKey.OBJECT,
        new HashMap<String, Object>() {
          {
            put(JsonKey.ID, id);
            put(JsonKey.TYPE, InstructionEvent.CERTIFICATE_GENERATOR.getType());
          }
        });

    //data.put(CourseJsonKey.ACTION, InstructionEvent.ISSUE_COURSE_CERTIFICATE.getAction());
    Object[] dataObj = new Object[1];
    dataObj[0] = new HashMap<String, Object>() {
      {
        put(JsonKey.RECIPIENT_NAME,"Creation");
        put(JsonKey.RECIPIENT_ID,userIds.get(0));
      }
    };
    Object[] publicKeyObj = new Object[2];
    publicKeyObj[0] = "1";
    publicKeyObj[1] = "2";

    Map<String,Object> issuerObj = new HashMap<String,Object>();
    issuerObj.put(JsonKey.NAME, "UPSMF trainning and research program");
    issuerObj.put(JsonKey.URL, "https://gcert.gujarat.gov.in/gcert/");
    issuerObj.put("publicKey", publicKeyObj);

    Map<String,String> signatoryListMap = new HashMap<String,String>();
    signatoryListMap.put(JsonKey.NAME,"Regulator UPSMF");
    signatoryListMap.put(JsonKey.ID,"Regulator");
    signatoryListMap.put("designation","Regulator");
    signatoryListMap.put(JsonKey.IMAGE, "https://cdn.pixabay.com/photo/2014/11/09/08/06/signature-523237__340.jpg");
    Object[] signatoryListObj = new Object[1];
    signatoryListObj[0] = signatoryListMap;

    Map<String,String> criteriaMap = new HashMap<String,String>();
    criteriaMap.put("narrative", "course completion certificate");

    Map<String,String> relatedMap = new HashMap<String,String>();
    relatedMap.put(JsonKey.TYPE, "course");
    relatedMap.put(JsonKey.BATCH_ID, batchId);
    relatedMap.put(JsonKey.COURSE_ID, courseId);


    data.put(
        CourseJsonKey.E_DATA,
        new HashMap<String, Object>() {
          {
//            if (CollectionUtils.isNotEmpty(userIds)) {
//              put(JsonKey.USER_IDs, userIds);
//            }
            //put(JsonKey.BATCH_ID, batchId);
            put(JsonKey.USER_ID, userIds.get(0));
            put("svgTemplate", "https://ntpstagingall.blob.core.windows.net/user/cert/File-01311849840255795242.svg");
            put(JsonKey.TEMPLATE_ID, "template_01_dev_001");
            put(JsonKey.COURSE_NAME, "new course june29");
            put(JsonKey.DATA, dataObj);
            put(JsonKey.NAME, "100PercentCompletionCertificate");
            put(JsonKey.TAG, batchId);
            put("issuer", issuerObj);
            put("signatoryList", signatoryListObj);
            put("criteria", criteriaMap);
            put( "basePath", "https://dev.sunbirded.org/certs");
            put( "related", relatedMap);
            //put(JsonKey.COURSE_ID, courseId);
            //put(CourseJsonKey.ACTION, InstructionEvent.ISSUE_COURSE_CERTIFICATE.getAction());
            //put(CourseJsonKey.ITERATION, 1);

//            if (reIssue) {
//              put(CourseJsonKey.REISSUE, true);
//            }
          }
        });
    System.out.println("Edata json is "+ data.toString());
    String topic = "sunbirddev.generate.certificate.request";
    InstructionEventGenerator.pushInstructionEvent(batchId, topic, data);
  }
}

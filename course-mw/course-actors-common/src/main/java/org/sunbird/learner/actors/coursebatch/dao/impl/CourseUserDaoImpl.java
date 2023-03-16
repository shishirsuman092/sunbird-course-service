package org.sunbird.learner.actors.coursebatch.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.CassandraPropertyReader;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.CourseUserDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.course.user.CourseUser;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseUserDaoImpl implements CourseUserDao {

    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private Util.DbInfo courseUserDb = Util.dbInfoMap.get(JsonKey.COURSE_USER_DB);
    private static final CassandraPropertyReader propertiesCache =
            CassandraPropertyReader.getInstance();
    private ObjectMapper mapper = new ObjectMapper();


    @Override
    public Response insert(RequestContext requestContext, Map<String, Object> courseUserDetails){
        return cassandraOperation.insertRecord(
                requestContext, courseUserDb.getKeySpace(), courseUserDb.getTableName(),courseUserDetails);
    }

    @Override
    public CourseUser read(RequestContext requestContext, String courseId,List<String> userId) {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.COURSE_ID, courseId);
        Response response = cassandraOperation.getRecordByIdentifier(requestContext, courseUserDb.getKeySpace(), courseUserDb.getTableName(), primaryKey, null);
        List<Map<String, Object>> CourseUserList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isEmpty(CourseUserList)) {
            return null;
        }
        try {
            return mapper.convertValue((Map<String, Object>) CourseUserList.get(0), CourseUser.class);
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public Response update(RequestContext requestContext, String courseId, String userId, Map<String, Object> map) {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.COURSE_ID, courseId);
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.putAll(map);
        attributeMap.remove(JsonKey.COURSE_ID);
        attributeMap = CassandraUtil.changeCassandraColumnMapping(attributeMap);
        return cassandraOperation.updateRecord(
                requestContext, courseUserDb.getKeySpace(), courseUserDb.getTableName(), attributeMap, primaryKey);
    }




}

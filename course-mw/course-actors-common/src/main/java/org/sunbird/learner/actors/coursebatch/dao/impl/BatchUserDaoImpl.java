package org.sunbird.learner.actors.coursebatch.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.BatchUserDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.batch.user.BatchUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchUserDaoImpl implements BatchUserDao {


    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private Util.DbInfo batchUserDb = Util.dbInfoMap.get(JsonKey.BATCH_USER_DB);
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public BatchUser read(RequestContext requestContext, String batchId, List<String> userId) {

        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.BATCH_ID, batchId);
        Response response = cassandraOperation.getRecordByIdentifier(requestContext, batchUserDb.getKeySpace(), batchUserDb.getTableName(), primaryKey, null);
        List<Map<String, Object>> batchUserList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isEmpty(batchUserList)) {
            return null;
        }
        try {
            return mapper.convertValue((Map<String, Object>) batchUserList.get(0), BatchUser.class);
        } catch (Exception e) {

            return null;
        }
    }

        @Override
        public Response insert (RequestContext requestContext, Map < String, Object > batchUserDetails){
            return cassandraOperation.insertRecord(requestContext, batchUserDb.getKeySpace(), batchUserDb.getTableName(), batchUserDetails);
        }
    @Override
    public Response update(RequestContext requestContext, String batchId,String userId, Map<String, Object> map) {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.BATCH_ID, batchId);
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.putAll(map);
        attributeMap.remove(JsonKey.BATCH_ID);
        attributeMap = CassandraUtil.changeCassandraColumnMapping(attributeMap);
        return cassandraOperation.updateRecord(
                requestContext, batchUserDb.getKeySpace(), batchUserDb.getTableName(), attributeMap, primaryKey);
    }
}

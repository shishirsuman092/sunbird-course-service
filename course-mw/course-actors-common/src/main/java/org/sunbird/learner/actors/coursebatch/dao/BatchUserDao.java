package org.sunbird.learner.actors.coursebatch.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.batch.user.BatchUser;

import java.util.List;
import java.util.Map;

public interface BatchUserDao {


    /**
     * Get batch user information.
     *
     * @param batchId,userId batch user identifiers
     * @param requestContext
     * @return User batch information
     */
    BatchUser read(RequestContext requestContext, String batchId, List<String> userId);

    /**
     * Create an entry for  user batch information
     *
     * @param requestContext
     * @param batchUserDetails  user batch information
     */
    Response insert(RequestContext requestContext, Map<String, Object> batchUserDetails);

    /**
     * Update batch user.
     *
     * @param batchUserMap batch user information to be updated
     * @return Response containing status of batch user update
     */
    Response update(RequestContext requestContext,String batchId,String userId, Map<String, Object> batchUserMap);

}

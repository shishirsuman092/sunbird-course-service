package org.sunbird.models.course.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseUser implements Serializable {

  private static final long serialVersionUID = 1L;
  private String courseId;

  private List<String> userId;


  public String getCourseId() {
    return courseId;
  }

  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }


  public List<String> getUserId() {
    return userId;
  }

  public void setUserId(List<String> userId) {
    this.userId = userId;
  }
}

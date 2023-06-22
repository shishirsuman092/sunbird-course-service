package org.sunbird.models.course.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseUser implements Serializable {

  private static final long serialVersionUID = 1L;
  private String courseid;

  private String userid;

  private String username;

  private String name;

  private int status;

  private Date enrolledDate;

  private Map<String,String> comment;

  public Map<String, String> getComment() {
    return comment;
  }

  public void setComment(Map<String, String> comment) {
    this.comment = comment;
  }

  public String getCourseId() {
    return courseid;
  }

  public void setCourseId(String courseId) {
    this.courseid = courseid;
  }


  public String getUserId() {
    return userid;
  }

  public void setUserId(String userid) {
    this.userid = userid;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public Date getEnrolledDate() {
    return enrolledDate;
  }

  public void setEnrolledDate(Date enrolledDate) {
    this.enrolledDate = enrolledDate;
  }
}

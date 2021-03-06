package com.kidozh.discuzhub.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Ignore;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForumInfo implements Serializable {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public int fid, fup, threads, posts;
    public String description, rules, name, password;
    @JsonProperty("picstyle")
    public String picStyle;
    @JsonProperty("autoclose")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public int autoClose;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty("threadcount")
    public int threadCount;
    @JsonProperty("icon")
    public String iconUrl;
    @JsonProperty("todayposts")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public int todayPosts = 0;
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonProperty("sublist")
    public List<ForumInfo> subForumList;
    @JsonProperty("redirect")
    public String redirectURL;

}

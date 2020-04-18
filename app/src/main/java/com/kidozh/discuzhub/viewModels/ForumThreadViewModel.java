package com.kidozh.discuzhub.viewModels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kidozh.discuzhub.database.bbsThreadDraftDatabase;
import com.kidozh.discuzhub.entities.bbsInformation;
import com.kidozh.discuzhub.entities.forumInfo;
import com.kidozh.discuzhub.entities.forumUserBriefInfo;
import com.kidozh.discuzhub.entities.threadInfo;
import com.kidozh.discuzhub.utilities.bbsParseUtils;
import com.kidozh.discuzhub.utilities.bbsURLUtils;
import com.kidozh.discuzhub.utilities.networkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForumThreadViewModel extends AndroidViewModel {
    private String TAG = ForumThreadViewModel.class.getSimpleName();

    public MutableLiveData<bbsURLUtils.ForumStatus> forumStatusMutableLiveData;

    bbsInformation curBBS;
    forumUserBriefInfo curUser;
    forumInfo forum;
    private OkHttpClient client;

    public MutableLiveData<Boolean> isLoading, isError, hasLoadAll;
    public MutableLiveData<List<threadInfo>> threadInfoListMutableLiveData;
    public MutableLiveData<String> jsonString, forumDescription;
    public LiveData<Integer> draftNumberLiveData;


    public ForumThreadViewModel(@NonNull Application application) {
        super(application);
        forumStatusMutableLiveData = new MutableLiveData<bbsURLUtils.ForumStatus>();
        isLoading = new MutableLiveData<Boolean>(false);
        isError = new MutableLiveData<Boolean>(false);
        isError.setValue(false);
        isLoading.setValue(false);
        jsonString = new MutableLiveData<>();
        forumDescription = new MutableLiveData<>("");
        hasLoadAll = new MutableLiveData<>(false);
        draftNumberLiveData = bbsThreadDraftDatabase
                .getInstance(getApplication())
                .getbbsThreadDraftDao()
                .getDraftNumber();
    }

    public void setBBSInfo(bbsInformation bbsInfo, forumUserBriefInfo userBriefInfo, forumInfo forum){
        this.curBBS = bbsInfo;
        this.curUser = userBriefInfo;
        this.forum = forum;
        client = networkUtils.getPreferredClientWithCookieJarByUser(getApplication(),userBriefInfo);
    }

    public LiveData<List<threadInfo>> getThreadInfoListLiveData(){
        if(threadInfoListMutableLiveData == null){
            threadInfoListMutableLiveData = new MutableLiveData<>();
            bbsURLUtils.ForumStatus forumStatus = new bbsURLUtils.ForumStatus(forum.fid,1);
            setForumStatusAndFetchThread(forumStatus);
        }
        return threadInfoListMutableLiveData;
    }

    public void setForumStatusAndFetchThread(bbsURLUtils.ForumStatus forumStatus){
        forumStatusMutableLiveData.postValue(forumStatus);
        getThreadList(forumStatus);
    }

    public void getThreadList(bbsURLUtils.ForumStatus forumStatus){
        isError.postValue(false);
        isLoading.postValue(true);
        Request request = new Request.Builder()
                .url(bbsURLUtils.getForumUrlByStatus(forumStatus))
                .build();
        Log.d(TAG,"Send request to "+bbsURLUtils.getForumUrlByStatus(forumStatus));

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isLoading.postValue(false);
                isError.postValue(true);
                // clear status if page == 1
                if(forumStatus.page == 1){
                    threadInfoListMutableLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isLoading.postValue(false);
                // clear status if page == 1
                if(forumStatus.page == 1){
                    threadInfoListMutableLiveData.postValue(new ArrayList<>());
                }
                List<threadInfo> threadInfoList;
                if(response.isSuccessful() && response.body()!=null){
                    String s = response.body().string();
                    jsonString.postValue(s);
                    Log.d(TAG,"recv forum thread json "+s);
                    if(forumStatus.page == 1){
                        threadInfoList = bbsParseUtils.parseThreadListInfo(s,true);
                    }
                    else {
                        threadInfoList = bbsParseUtils.parseThreadListInfo(s,false);
                    }
                    List<threadInfo> currentThreadInfo = threadInfoListMutableLiveData.getValue();
                    if(currentThreadInfo == null){
                        currentThreadInfo = new ArrayList<>();
                    }

                    if(threadInfoList == null){
                        isError.postValue(true);
                        if(forumStatus.page != 1){
                            // rollback
                            forumStatus.page -= 1;
                            forumStatusMutableLiveData.postValue(forumStatus);
                        }
                    }
                    else {
                        currentThreadInfo.addAll(threadInfoList);
                        threadInfoListMutableLiveData.postValue(currentThreadInfo);
                        Log.d(TAG,"recv thread "+threadInfoList.size()+" status ppp "+forumStatus.perPage);
                        if(threadInfoList.size() < forumStatus.perPage){

                            hasLoadAll.postValue(true);
                            if(forumStatus.page != 1){
                                // rollback
                                forumStatus.page -= 1;
                                forumStatusMutableLiveData.postValue(forumStatus);
                            }
                        }
                    }
                    // refresh description if possible
                    if (bbsParseUtils.getThreadRuleString(s)!=null && bbsParseUtils.getThreadRuleString(s).equals("")){
                        if(!forumDescription.getValue().equals(bbsParseUtils.getThreadDescriptionString(s))){
                            forumDescription.postValue(bbsParseUtils.getThreadDescriptionString(s));
                        }

                    }
                    else {
                        if(!forumDescription.getValue().equals(bbsParseUtils.getThreadRuleString(s))){
                            forumDescription.postValue(bbsParseUtils.getThreadRuleString(s));
                        }

                    }


                }
            }
        });

    }



}
package com.android.hugoar.VuforiaSamples.video;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import com.android.hugoar.SampleApplication.SampleApplicationSession;
import com.android.hugoar.SampleApplication.utils.SampleMath;
import com.android.hugoar.SampleApplication.utils.SampleUtils;
import com.android.hugoar.SampleApplication.utils.Texture;
import com.android.hugoar.VuforiaSamples.app.ImageTargets.ImageTargets;
import com.vuforia.ImageTarget;
import com.vuforia.Matrix44F;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

/**
 * Created by hugo on 5/14/16.
 */
public class VideoModel {
    public ImageTargets activity;
    private VideoPlayerHelper mVideoPlayerHelper[] = null;
    private String mMovieName[] = null;
    private VideoPlayerHelper.MEDIA_TYPE mCanRequestType[] = null;
    private int mSeekPosition[] = null;
    private boolean mShouldPlayImmediately[] = null;
    private long mLostTrackingSince[] = null;
    private boolean mLoadRequested[] = null;
    private float[][] mTexCoordTransformationMatrix = null;


    // Needed to calculate whether a screen tap is inside the target
    Matrix44F modelViewMatrix[] = null;
    // Trackable dimensions
    Vec3F targetPositiveDimensions[] = null;
    // Video Playback Textures for the two targets
    int videoPlaybackTextureID[] = null;

    // Video Playback Rendering Specific
    private int videoPlaybackShaderID = 0;
    private int videoPlaybackVertexHandle = 0;
    private int videoPlaybackNormalHandle = 0;
    private int videoPlaybackTexCoordHandle = 0;
    private int videoPlaybackMVPMatrixHandle = 0;
    private int videoPlaybackTexSamplerOESHandle = 0;

    // Keyframe and icon rendering specific
    private int keyframeShaderID = 0;
    private int keyframeVertexHandle = 0;
    private int keyframeNormalHandle = 0;
    private int keyframeTexCoordHandle = 0;
    private int keyframeMVPMatrixHandle = 0;
    private int keyframeTexSampler2DHandle = 0;

    // These hold the aspect ratio of both the video and the
    // keyframe
    float videoQuadAspectRatio[] = null;
    float keyframeQuadAspectRatio[] = null;

    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;

    double quadVerticesArray[] = { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f, 1.0f, 0.0f };

    double quadTexCoordsArray[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            1.0f };

    double quadNormalsArray[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, };

    short quadIndicesArray[] = { 0, 1, 2, 2, 3, 0 };

    // We cannot use the default texture coordinates of the quad since these
    // will change depending on the video itself
    private float videoQuadTextureCoords[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f, };

    // This variable will hold the transformed coordinates (changes every frame)
    private float videoQuadTextureCoordsTransformedStones[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };

    private float videoQuadTextureCoordsTransformedChips[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };

    VideoPlayerHelper.MEDIA_STATE currentStatus[] = null;

    static int NUM_QUAD_INDEX = 6;

    // The textures we will use for rendering:
    public Vector<Texture> mTextures;

    boolean isTracking[] = null;

    public VideoModel(ImageTargets activity){
        this.activity = activity;
        isTracking = new boolean[activity.videoControl.NUM_TARGETS];
        modelViewMatrix = new Matrix44F[activity.videoControl.NUM_TARGETS];
        targetPositiveDimensions = new Vec3F[activity.videoControl.NUM_TARGETS];
        videoPlaybackTextureID = new int[activity.videoControl.NUM_TARGETS];
        videoQuadAspectRatio = new float[activity.videoControl.NUM_TARGETS];
        keyframeQuadAspectRatio = new float[activity.videoControl.NUM_TARGETS];
        currentStatus = new VideoPlayerHelper.MEDIA_STATE[activity.videoControl.NUM_TARGETS];

        init();

    }

    // Store the Player Helper object passed from the main activity
    public void setVideoPlayerHelper(int target, VideoPlayerHelper newVideoPlayerHelper)
    {
        mVideoPlayerHelper[target] = newVideoPlayerHelper;
    }

    private void init() {

        mVideoPlayerHelper = new VideoPlayerHelper[activity.videoControl.NUM_TARGETS];
        mMovieName = new String[activity.videoControl.NUM_TARGETS];
        mCanRequestType = new VideoPlayerHelper.MEDIA_TYPE[activity.videoControl.NUM_TARGETS];
        mSeekPosition = new int[activity.videoControl.NUM_TARGETS];
        mShouldPlayImmediately = new boolean[activity.videoControl.NUM_TARGETS];
        mLostTrackingSince = new long[activity.videoControl.NUM_TARGETS];
        mLoadRequested = new boolean[activity.videoControl.NUM_TARGETS];
        mTexCoordTransformationMatrix = new float[activity.videoControl.NUM_TARGETS][16];

        // Initialize the arrays to default values
        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
        {
            mVideoPlayerHelper[i] = null;
            mMovieName[i] = "";
            mCanRequestType[i] = VideoPlayerHelper.MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
            mSeekPosition[i] = 0;
            mShouldPlayImmediately[i] = false;
            mLostTrackingSince[i] = -1;
            mLoadRequested[i] = false;
        }

        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
            targetPositiveDimensions[i] = new Vec3F();

        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
            modelViewMatrix[i] = new Matrix44F();

    }

    public void initModel(){

        mTextures = new Vector<Texture>();
        loadTextures();
        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            // Here we create the textures for the keyframe
            // and for all the icons
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        // Now we create the texture for the video data from the movie
        // IMPORTANT:
        // Notice that the textures are not typical GL_TEXTURE_2D textures
        // but instead are GL_TEXTURE_EXTERNAL_OES extension textures
        // This is required by the Android SurfaceTexture
        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
        {
            GLES20.glGenTextures(1, videoPlaybackTextureID, i);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    videoPlaybackTextureID[i]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }

        // The first shader is the one that will display the video data of the
        // movie
        // (it is aware of the GL_TEXTURE_EXTERNAL_OES extension)
        videoPlaybackShaderID = SampleUtils.createProgramFromShaderSrc(
                VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
                VideoPlaybackShaders.VIDEO_PLAYBACK_FRAGMENT_SHADER);
        videoPlaybackVertexHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexPosition");
        videoPlaybackNormalHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexNormal");
        videoPlaybackTexCoordHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexTexCoord");
        videoPlaybackMVPMatrixHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "modelViewProjectionMatrix");
        videoPlaybackTexSamplerOESHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "texSamplerOES");

        // This is a simpler shader with regular 2D textures
        keyframeShaderID = SampleUtils.createProgramFromShaderSrc(
                KeyFrameShaders.KEY_FRAME_VERTEX_SHADER,
                KeyFrameShaders.KEY_FRAME_FRAGMENT_SHADER);
        keyframeVertexHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexPosition");
        keyframeNormalHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexNormal");
        keyframeTexCoordHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexTexCoord");
        keyframeMVPMatrixHandle = GLES20.glGetUniformLocation(keyframeShaderID,
                "modelViewProjectionMatrix");
        keyframeTexSampler2DHandle = GLES20.glGetUniformLocation(
                keyframeShaderID, "texSampler2D");

        keyframeQuadAspectRatio[activity.videoControl.STONES] = (float) mTextures
                .get(0).mHeight / (float) mTextures.get(0).mWidth;
        keyframeQuadAspectRatio[activity.videoControl.CHIPS] = (float) mTextures.get(1).mHeight
                / (float) mTextures.get(1).mWidth;

        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);

        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
        {

            if (mVideoPlayerHelper[i] != null)
            {
                // The VideoPlayerHelper needs to setup a surface texture given
                // the texture id
                // Here we inform the video player that we would like to play
                // the movie
                // both on texture and on full screen
                // Notice that this does not mean that the platform will be able
                // to do what we request
                // After the file has been loaded one must always check with
                // isPlayableOnTexture() whether
                // this can be played embedded in the AR scene
                if (!mVideoPlayerHelper[i]
                        .setupSurfaceTexture(videoPlaybackTextureID[i]))
                    mCanRequestType[i] = VideoPlayerHelper.MEDIA_TYPE.FULLSCREEN;
                else
                    mCanRequestType[i] = VideoPlayerHelper.MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;

                // And now check if a load has been requested with the
                // parameters passed from the main activity
                if (mLoadRequested[i])
                {
                    mVideoPlayerHelper[i].load(mMovieName[i],
                            mCanRequestType[i], mShouldPlayImmediately[i],
                            mSeekPosition[i]);
                    mLoadRequested[i] = false;
                }
            }
        }
    }

    public void surfaceChanged(){
        // Upon every on pause the movie had to be unloaded to release resources
        // Thus, upon every surface create or surface change this has to be
        // reloaded
        // See:
        // http://developer.android.com/reference/android/media/MediaPlayer.html#release()
        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
        {
            if (mLoadRequested[i] && mVideoPlayerHelper[i] != null)
            {
                mVideoPlayerHelper[i].load(mMovieName[i], mCanRequestType[i],
                        mShouldPlayImmediately[i], mSeekPosition[i]);
                mLoadRequested[i] = false;
            }
        }
    }

    public void beforeDrawFrame(){
        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
        {
            if (mVideoPlayerHelper[i] != null)
            {
                if (mVideoPlayerHelper[i].isPlayableOnTexture())
                {
                    // First we need to update the video data. This is a built
                    // in Android call
                    // Here, the decoded data is uploaded to the OES texture
                    // We only need to do this if the movie is playing
                    if (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING)
                    {
                        mVideoPlayerHelper[i].updateVideoData();
                    }

                    // According to the Android API
                    // (http://developer.android.com/reference/android/graphics/SurfaceTexture.html)
                    // transforming the texture coordinates needs to happen
                    // every frame.
                    mVideoPlayerHelper[i]
                            .getSurfaceTextureTransformMatrix(mTexCoordTransformationMatrix[i]);
                    setVideoDimensions(i,
                            mVideoPlayerHelper[i].getVideoWidth(),
                            mVideoPlayerHelper[i].getVideoHeight(),
                            mTexCoordTransformationMatrix[i]);
                }

                setStatus(i, mVideoPlayerHelper[i].getStatus().getNumericType());
            }
        }
    }

    boolean isTracking(int target)
    {
        return isTracking[target];
    }

    public void afterDrawFrame(){
        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
        {
            // Ask whether the target is currently being tracked and if so react
            // to it
            if (isTracking(i))
            {
                // If it is tracking reset the timestamp for lost tracking
                mLostTrackingSince[i] = -1;
            } else
            {
                // If it isn't tracking
                // check whether it just lost it or if it's been a while
                if (mLostTrackingSince[i] < 0)
                    mLostTrackingSince[i] = SystemClock.uptimeMillis();
                else
                {
                    // If it's been more than 2 seconds then pause the player
                    if ((SystemClock.uptimeMillis() - mLostTrackingSince[i]) > 2000)
                    {
                        if (mVideoPlayerHelper[i] != null)
                            mVideoPlayerHelper[i].pause();
                    }
                }
            }
        }

        // If you would like the video to start playing as soon as it starts
        // tracking
        // and pause as soon as tracking is lost you can do that here by
        // commenting
        // the for-loop above and instead checking whether the isTracking()
        // value has
        // changed since the last frame. Notice that you need to be careful not
        // to
        // trigger automatic playback for fullscreen since that will be
        // inconvenient
        // for your users.
    }

    public void drawSelf(Trackable trackable, SampleApplicationSession vuforiaAppSession,TrackableResult result){


        float temp[] = { 0.0f, 0.0f, 0.0f };
        for (int i = 0; i < activity.videoControl.NUM_TARGETS; i++)
        {
            isTracking[i] = false;
            targetPositiveDimensions[i].setData(temp);
        }
        int currentTarget;
        currentTarget = activity.videoControl.CHIPS;
        modelViewMatrix[currentTarget] = Tool.convertPose2GLMatrix(result.getPose());

        isTracking[currentTarget] = true;
        targetPositiveDimensions[currentTarget] = ((ImageTarget)trackable).getSize();

        // The pose delivers the center of the target, thus the dimensions
        // go from -width/2 to width/2, same for height
        temp[0] = targetPositiveDimensions[currentTarget].getData()[0] / 2.0f;
        temp[1] = targetPositiveDimensions[currentTarget].getData()[1] / 2.0f;
        targetPositiveDimensions[currentTarget].setData(temp);

        // If the movie is ready to start playing or it has reached the end
        // of playback we render the keyframe
        if ((currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.ERROR))
        {
            float[] modelViewMatrixKeyframe = Tool.convertPose2GLMatrix(
                    result.getPose()).getData();
            float[] modelViewProjectionKeyframe = new float[16];
            // Matrix.translateM(modelViewMatrixKeyframe, 0, 0.0f, 0.0f,
            // targetPositiveDimensions[currentTarget].getData()[0]);

            // Here we use the aspect ratio of the keyframe since it
            // is likely that it is not a perfect square

            float ratio = 1.0f;
            if (mTextures.get(currentTarget).mSuccess)
                ratio = keyframeQuadAspectRatio[currentTarget];
            else
                ratio = targetPositiveDimensions[currentTarget].getData()[1]
                        / targetPositiveDimensions[currentTarget].getData()[0];

            Matrix.scaleM(modelViewMatrixKeyframe, 0,
                    targetPositiveDimensions[currentTarget].getData()[0],
                    targetPositiveDimensions[currentTarget].getData()[0]
                            * ratio,
                    targetPositiveDimensions[currentTarget].getData()[0]);
            Matrix.multiplyMM(modelViewProjectionKeyframe, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixKeyframe, 0);

            GLES20.glUseProgram(keyframeShaderID);

            // Prepare for rendering the keyframe
            GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);
            GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadNormals);
            GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, quadTexCoords);

            GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
            GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
            GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // The first loaded texture from the assets folder is the
            // keyframe
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    mTextures.get(currentTarget).mTextureID[0]);
            GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                    modelViewProjectionKeyframe, 0);
            GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);

            // Render
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);

            GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
            GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
            GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);

            GLES20.glUseProgram(0);
        } else
        // In any other case, such as playing or paused, we render
        // the actual contents
        {
            float[] modelViewMatrixVideo = Tool.convertPose2GLMatrix(
                    result.getPose()).getData();
            float[] modelViewProjectionVideo = new float[16];
            // Matrix.translateM(modelViewMatrixVideo, 0, 0.0f, 0.0f,
            // targetPositiveDimensions[currentTarget].getData()[0]);

            // Here we use the aspect ratio of the video frame
            Matrix.scaleM(modelViewMatrixVideo, 0,
                    targetPositiveDimensions[currentTarget].getData()[0],
                    targetPositiveDimensions[currentTarget].getData()[0]
                            * videoQuadAspectRatio[currentTarget],
                    targetPositiveDimensions[currentTarget].getData()[0]);
            Matrix.multiplyMM(modelViewProjectionVideo, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixVideo, 0);

            GLES20.glUseProgram(videoPlaybackShaderID);

            // Prepare for rendering the keyframe
            GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);
            GLES20.glVertexAttribPointer(videoPlaybackNormalHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadNormals);

            if (trackable.getName().compareTo("stones") == 0)
                GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                        2, GLES20.GL_FLOAT, false, 0,
                        fillBuffer(videoQuadTextureCoordsTransformedStones));
            else
                GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                        2, GLES20.GL_FLOAT, false, 0,
                        fillBuffer(videoQuadTextureCoordsTransformedChips));

            GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
            GLES20.glEnableVertexAttribArray(videoPlaybackNormalHandle);
            GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // IMPORTANT:
            // Notice here that the texture that we are binding is not the
            // typical GL_TEXTURE_2D but instead the GL_TEXTURE_EXTERNAL_OES
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    videoPlaybackTextureID[currentTarget]);
            GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1,
                    false, modelViewProjectionVideo, 0);
            GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);

            // Render
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);

            GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
            GLES20.glDisableVertexAttribArray(videoPlaybackNormalHandle);
            GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);

            GLES20.glUseProgram(0);

        }

        // The following section renders the icons. The actual textures used
        // are loaded from the assets folder

        if ((currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.ERROR))
        {
            // If the movie is ready to be played, pause, has reached end or
            // is not
            // ready then we display one of the icons
            float[] modelViewMatrixButton = Tool.convertPose2GLMatrix(
                    result.getPose()).getData();
            float[] modelViewProjectionButton = new float[16];

            GLES20.glDepthFunc(GLES20.GL_LEQUAL);

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA);

            // The inacuracy of the rendering process in some devices means
            // that
            // even if we use the "Less or Equal" version of the depth
            // function
            // it is likely that we will get ugly artifacts
            // That is the translation in the Z direction is slightly
            // different
            // Another posibility would be to use a depth func "ALWAYS" but
            // that is typically not a good idea
            Matrix
                    .translateM(
                            modelViewMatrixButton,
                            0,
                            0.0f,
                            0.0f,
                            targetPositiveDimensions[currentTarget].getData()[1] / 10.98f);
            Matrix
                    .scaleM(
                            modelViewMatrixButton,
                            0,
                            (targetPositiveDimensions[currentTarget].getData()[1] / 2.0f),
                            (targetPositiveDimensions[currentTarget].getData()[1] / 2.0f),
                            (targetPositiveDimensions[currentTarget].getData()[1] / 2.0f));
            Matrix.multiplyMM(modelViewProjectionButton, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixButton, 0);

            GLES20.glUseProgram(keyframeShaderID);

            GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);
            GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadNormals);
            GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, quadTexCoords);

            GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
            GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
            GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // Depending on the status in which we are we choose the
            // appropriate
            // texture to display. Notice that unlike the video these are
            // regular
            // GL_TEXTURE_2D textures
            switch (currentStatus[currentTarget])
            {
                case READY:
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                    break;
                case REACHED_END:
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                    break;
                case PAUSED:
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                    break;
                case NOT_READY:
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(3).mTextureID[0]);
                    break;
                case ERROR:
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(4).mTextureID[0]);
                    break;
                default:
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(3).mTextureID[0]);
                    break;
            }
            GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                    modelViewProjectionButton, 0);
            GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);

            // Render
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);

            GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
            GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
            GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);

            GLES20.glUseProgram(0);

            // Finally we return the depth func to its original state
            GLES20.glDepthFunc(GLES20.GL_LESS);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        SampleUtils.checkGLError("VideoPlayback renderFrame");
    }


    public boolean isTapOnScreenInsideTarget(int target, float x, float y)
    {
        // Here we calculate that the touch event is inside the target
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = SampleMath.getPointToPlaneIntersection(SampleMath
                        .Matrix44FInverse(activity.vuforiaAppSession.getProjectionMatrix()),
                modelViewMatrix[target], metrics.widthPixels, metrics.heightPixels,
                new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));

        // The target returns as pose the center of the trackable. The following
        // if-statement simply checks that the tap is within this range
        if ((intersection.getData()[0] >= -(targetPositiveDimensions[target]
                .getData()[0]))
                && (intersection.getData()[0] <= (targetPositiveDimensions[target]
                .getData()[0]))
                && (intersection.getData()[1] >= -(targetPositiveDimensions[target]
                .getData()[1]))
                && (intersection.getData()[1] <= (targetPositiveDimensions[target]
                .getData()[1]))) {
            return true;
        }else {
            return false;
        }
    }

    // Multiply the UV coordinates by the given transformation matrix
    float[] uvMultMat4f(float transformedU, float transformedV, float u,
                        float v, float[] pMat)
    {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */+ pMat[12]
                * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */+ pMat[13]
                * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f; // We
        // dont need z and w so we comment them out
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;

        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }


    void setStatus(int target, int value) {
        // Transform the value passed from java to our own values
        switch (value) {
            case 0:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.REACHED_END;
                break;
            case 1:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PAUSED;
                break;
            case 2:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.STOPPED;
                break;
            case 3:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PLAYING;
                break;
            case 4:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.READY;
                break;
            case 5:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
            case 6:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.ERROR;
                break;
            default:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
        }
    }

    private void setVideoDimensions(int target, float videoWidth, float videoHeight,
                            float[] textureCoordMatrix)
    {
        // The quad originaly comes as a perfect square, however, the video
        // often has a different aspect ration such as 4:3 or 16:9,
        // To mitigate this we have two options:
        // 1) We can either scale the width (typically up)
        // 2) We can scale the height (typically down)
        // Which one to use is just a matter of preference. This example scales
        // the height down.
        // (see the render call in renderFrame)
        videoQuadAspectRatio[target] = videoHeight / videoWidth;

        float mtx[] = textureCoordMatrix;
        float tempUVMultRes[] = new float[2];

        if (target == activity.videoControl.STONES)
        {
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedStones[0],
                    videoQuadTextureCoordsTransformedStones[1],
                    videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformedStones[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedStones[2],
                    videoQuadTextureCoordsTransformedStones[3],
                    videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformedStones[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedStones[4],
                    videoQuadTextureCoordsTransformedStones[5],
                    videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformedStones[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedStones[6],
                    videoQuadTextureCoordsTransformedStones[7],
                    videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformedStones[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[7] = tempUVMultRes[1];
        } else if (target == activity.videoControl.CHIPS)
        {
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedChips[0],
                    videoQuadTextureCoordsTransformedChips[1],
                    videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformedChips[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedChips[2],
                    videoQuadTextureCoordsTransformedChips[3],
                    videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformedChips[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedChips[4],
                    videoQuadTextureCoordsTransformedChips[5],
                    videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformedChips[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformedChips[6],
                    videoQuadTextureCoordsTransformedChips[7],
                    videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformedChips[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[7] = tempUVMultRes[1];
        }

        // textureCoordMatrix = mtx;
    }


    private Buffer fillBuffer(double[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();

        return bb;

    }


    private Buffer fillBuffer(short[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
        // short
        // takes 2
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();

        return bb;

    }


    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;

    }


    public void requestLoad(int target, String movieName, int seekPosition,
                            boolean playImmediately)
    {
        mMovieName[target] = movieName;
        mSeekPosition[target] = seekPosition;
        mShouldPlayImmediately[target] = playImmediately;
        mLoadRequested[target] = true;
    }

    // We want to load specific textures from the APK, which we will later
    // use for rendering.
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk(
                "VideoPlayback/VuforiaSizzleReel_1.png", activity.getAssets()));
        mTextures.add(Texture.loadTextureFromApk(
                "VideoPlayback/VuforiaSizzleReel_2.png", activity.getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/play.png",
                activity.getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/busy.png",
                activity.getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/error.png",
                activity.getAssets()));
    }
}

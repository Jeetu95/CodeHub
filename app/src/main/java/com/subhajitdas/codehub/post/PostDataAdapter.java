package com.subhajitdas.codehub.post;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.subhajitdas.codehub.Constants;
import com.subhajitdas.codehub.R;
import com.subhajitdas.codehub.readPost.ReadPost;

import java.util.ArrayList;

/**
 * Created by Subhajit Das on 12-06-2017.
 */

public class PostDataAdapter extends RecyclerView.Adapter<PostDataAdapter.ViewHolder> {

    private ArrayList<PostData> mDataSet;
    private DataSnapshot mLikeDataSnapshot,mBookmarkDataSnapshot;
    private DatabaseReference mRootRef;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        /* Details about the code below.
            View v- is the card view of our post.

            postTitle,
            posterName,
            postDate,
            postLike - text fields for data.

            likeButton,
            bookmarkButton - Like and bookmark buttons on our card layout .
         */
        TextView postTitle, posterName, postDate, postLike;
        LikeButton likeButton, bookmarkButton;
        CardView cardView;
        private Context context;

        public ViewHolder(View v) {
            super(v);
            postTitle = (TextView) v.findViewById(R.id.post_title);
            posterName = (TextView) v.findViewById(R.id.poster_name);
            postDate = (TextView) v.findViewById(R.id.post_date);
            postLike = (TextView) v.findViewById(R.id.post_like);
            likeButton = (LikeButton) v.findViewById(R.id.like_button);
            bookmarkButton = (LikeButton) v.findViewById(R.id.bookmark_button);
            cardView = (CardView) v.findViewById(R.id.card_view);
            context=v.getContext();
        }
    }

    // Constructor to get the data-set.
    public PostDataAdapter(ArrayList<PostData> data) {
        mDataSet = data;
        mRootRef = FirebaseDatabase.getInstance().getReference();
        //Offline for Like branch and storing data.
        mRootRef.child(Constants.LIKE).keepSynced(true);
        mRootRef.child(Constants.LIKE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mLikeDataSnapshot = dataSnapshot;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //Offline for Bookmark branch and storing data.
        mRootRef.child(Constants.BOOKMARK).keepSynced(true);
        mRootRef.child(Constants.BOOKMARK).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mBookmarkDataSnapshot = dataSnapshot;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //A new view is created.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.post_layout2, parent, false);

        //Returning the new created view.
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Filling in data in the fields of our card view.
        holder.postTitle.setText(mDataSet.get(position).data.title);
        holder.posterName.setText(mDataSet.get(position).data.userName);
        holder.postDate.setText(mDataSet.get(position).data.date);
        holder.postLike.setText(mDataSet.get(position).data.likes);
        //Setting up the two buttons.
        setLikeButton(holder, mDataSet.get(position).key);
        setBookmarkButton(holder,mDataSet.get(position).key);

        //Handling the like click
        holder.likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                //Writing data in Like branch.
                mRootRef.child(Constants.LIKE)
                        .child(mDataSet.get(position).key)
                        .child(currentUser.getUid())
                        .setValue(currentUser.getDisplayName());
                //Updating the like counter.
                mDataSet.get(position).data.likes=Integer.toString(Integer.parseInt(mDataSet.get(position).data.likes)+1);
                //Updating the like value in the post branch.
                mRootRef.child(Constants.PROGRAM)
                        .child(mDataSet.get(position).key)
                        .setValue(mDataSet.get(position).data);
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                //Removing data in Like branch.
                mRootRef.child(Constants.LIKE)
                        .child(mDataSet.get(position).key)
                        .child(currentUser.getUid())
                        .removeValue();
                //Updating the like counter.
                mDataSet.get(position).data.likes=Integer.toString(Integer.parseInt(mDataSet.get(position).data.likes)-1);
                //Updating the like value in the post branch.
                mRootRef.child(Constants.PROGRAM)
                        .child(mDataSet.get(position).key)
                        .setValue(mDataSet.get(position).data);
            }
        });

        //Handling the bookmark click
        holder.bookmarkButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                //Writing data to Bookmark branch.
                mRootRef.child(Constants.BOOKMARK)
                        .child(mDataSet.get(position).key)
                        .child(currentUser.getUid())
                        .setValue("yes");
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                //Removing data to Bookmark branch.
                mRootRef.child(Constants.BOOKMARK)
                        .child(mDataSet.get(position).key)
                        .child(currentUser.getUid())
                        .removeValue();
            }
        });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(holder.context, ReadPost.class);
                intent.putExtra(Constants.KEY,mDataSet.get(position).key);
                intent.putExtra(Constants.DATE,mDataSet.get(position).data.date);
                intent.putExtra(Constants.FILEUID,mDataSet.get(position).data.fileUid);
                intent.putExtra(Constants.FILEURI,mDataSet.get(position).data.fileUri);
                intent.putExtra(Constants.LIKES,mDataSet.get(position).data.likes);
                intent.putExtra(Constants.TITLE,mDataSet.get(position).data.title);
                intent.putExtra(Constants.USERID,mDataSet.get(position).data.userId);
                intent.putExtra(Constants.USERNAME,mDataSet.get(position).data.userName);
                holder.context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    //To set up the like button state in layout.
    public void setLikeButton(ViewHolder holder, String postKey) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mLikeDataSnapshot != null) {
            //Checking for post key.
            if (mLikeDataSnapshot.hasChild(postKey)) {
                //Checking if post has the current user.
                if (mLikeDataSnapshot.child(postKey).hasChild(currentUser.getUid())) {
                    holder.likeButton.setLiked(true);
                } else {
                    holder.likeButton.setLiked(false);
                }
            } else {
                holder.likeButton.setLiked(false);
            }
        }
    }
    //To set up the bookmark button state in layout.
    public void setBookmarkButton(ViewHolder holder, String postKey) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mBookmarkDataSnapshot != null) {
            // Checking for post key.
            if (mBookmarkDataSnapshot.hasChild(postKey)) {
                //Checking if post has the current user.
                if (mBookmarkDataSnapshot.child(postKey).hasChild(currentUser.getUid())) {
                    holder.bookmarkButton.setLiked(true);
                } else {
                    holder.bookmarkButton.setLiked(false);
                }
            } else {
                holder.bookmarkButton.setLiked(false);
            }
        }
    }

}

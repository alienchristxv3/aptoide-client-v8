package cm.aptoide.pt.v8engine.social.data.share;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cm.aptoide.accountmanager.Account;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.networking.image.ImageLoader;
import cm.aptoide.pt.v8engine.timeline.view.LikeButtonView;

public class SharePostViewSetup {
  private void setupBody(View view) {
    CardView cardView = (CardView) view.findViewById(R.id.card);
    LinearLayout like = (LinearLayout) view.findViewById(R.id.social_like);
    LikeButtonView likeButtonView = (LikeButtonView) view.findViewById(R.id.social_like_button);
    TextView comments = (TextView) view.findViewById(R.id.social_comment);
    LinearLayout socialInfoBar = (LinearLayout) view.findViewById(R.id.social_info_bar);
    LinearLayout socialCommentBar =
        (LinearLayout) view.findViewById(R.id.social_latest_comment_bar);

    cardView.setRadius(8);
    cardView.setCardElevation(10);
    like.setOnClickListener(null);
    like.setOnTouchListener(null);
    likeButtonView.setOnClickListener(null);
    likeButtonView.setOnTouchListener(null);
    like.setVisibility(View.VISIBLE);
    likeButtonView.setVisibility(View.VISIBLE);

    comments.setVisibility(View.VISIBLE);
    socialInfoBar.setVisibility(View.GONE);
    socialCommentBar.setVisibility(View.GONE);

    // yet to be used...
    // LinearLayout socialTerms = (LinearLayout) view.findViewById(R.id.social_privacy_terms);
    // TextView privacyText = (TextView) view.findViewById(R.id.social_text_privacy);
    // TextView numberOfComments = (TextView) view.findViewById(R.id.social_number_of_comments);
  }

  private void setupBottom(View view, Account account) {
    View socialPrivacyTerms = view.findViewById(R.id.social_privacy_terms);
    final boolean accessConfirmed = account.isAccessConfirmed();
    socialPrivacyTerms.setVisibility(accessConfirmed ? View.GONE : View.VISIBLE);
  }

  private void setupHeader(View view, Context context, Account account) {
    if (TextUtils.isEmpty(account.getStore()
        .getName())) {
      setupHeaderWithoutStoreName(view, context, account);
    } else {
      setupHeaderWithStoreName(view, context, account);
    }
  }

  private void setupHeaderWithStoreName(View view, Context context, Account account) {
    TextView storeName = (TextView) view.findViewById(R.id.card_title);
    TextView userName = (TextView) view.findViewById(R.id.card_subtitle);
    ImageView storeAvatar = (ImageView) view.findViewById(R.id.card_image);
    ImageView userAvatar = (ImageView) view.findViewById(R.id.card_user_avatar);

    final String accountStoreName = account.getStore()
        .getName();
    final String accountUserNickname = account.getNickname();
    final String accountUserAvatar = account.getAvatar();
    final String accountStoreAvatar = account.getStore()
        .getAvatar();
    final Account.Access accountUserAccess = account.getAccess();

    storeName.setTextColor(ContextCompat.getColor(context, R.color.black_87_alpha));
    if (Account.Access.PUBLIC.equals(accountUserAccess)) {
      storeName.setText(accountStoreName);
      storeAvatar.setVisibility(View.VISIBLE);
      userAvatar.setVisibility(View.VISIBLE);
      ImageLoader.with(context)
          .loadWithShadowCircleTransform(accountStoreAvatar, storeAvatar);
      ImageLoader.with(context)
          .loadWithShadowCircleTransform(accountUserAvatar, userAvatar);
      userName.setText(accountUserNickname);
    }
  }

  private void setupHeaderWithoutStoreName(View view, Context context, Account account) {
    TextView storeName = (TextView) view.findViewById(R.id.card_title);
    TextView userName = (TextView) view.findViewById(R.id.card_subtitle);
    ImageView storeAvatar = (ImageView) view.findViewById(R.id.card_image);
    ImageView userAvatar = (ImageView) view.findViewById(R.id.card_user_avatar);

    final String accountStoreName = account.getStore()
        .getName();
    final String accountStoreAvatar = account.getStore()
        .getAvatar();
    // final String accountUserAvatar = account.getAvatar();

    storeName.setText(accountStoreName);

    storeAvatar.setVisibility(View.VISIBLE);
    ImageLoader.with(context)
        .loadWithShadowCircleTransform(accountStoreAvatar, storeAvatar);

    userAvatar.setVisibility(View.INVISIBLE);
    // ImageLoader.with(context).loadWithShadowCircleTransform(accountUserAvatar, userAvatar);

    userName.setText(account.getNickname());
    userName.setVisibility(View.GONE);
  }

  public void setup(View view, Context context, Account account) {
    setupHeader(view, context, account);
    setupBody(view);
    setupBottom(view, account);
  }
}

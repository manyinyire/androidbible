package yuku.alkitab.base.ac;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import yuku.alkitab.R;
import yuku.alkitab.base.App;
import yuku.alkitab.base.U;
import yuku.alkitab.base.ac.base.BaseActivity;

public class ShareActivity extends BaseActivity {
	public static final String TAG = ShareActivity.class.getSimpleName();

	PackageManager mPm;
	ResolveListAdapter mAdapter;

	ListView lsIntent;

	public static Intent createIntent(Intent intent, String title) {
		Intent res = new Intent(App.context, ShareActivity.class);
		res.putExtra(Intent.EXTRA_INTENT, intent);
		res.putExtra(Intent.EXTRA_TITLE, title);
		return res;
	}
	
	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "1changing config: " + getChangingConfigurations());
	}
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share);
		
		String title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		setTitle(title);
		
		final Intent intent = getIntent().getParcelableExtra(Intent.EXTRA_INTENT);
		final Intent[] initialIntents = null;
		final List<ResolveInfo> rList = null;
		
		lsIntent = U.getView(this, R.id.lsIntent);
		
		mPm = getPackageManager();
		intent.setComponent(null);

		// kasih puter2 biar ga kaya hang
		final ProgressDialog pd = ProgressDialog.show(this, null, getString(R.string.please_wait_titik3), true, false);
		
		new AsyncTask<Void, Void, Void>() {
			@Override public Void doInBackground(Void... params) {
				mAdapter = new ResolveListAdapter(ShareActivity.this, intent, initialIntents, rList);
				return null;
			}
			
			@Override protected void onPostExecute(Void result) {
				lsIntent.setAdapter(mAdapter);
				pd.dismiss();
			}
		}.execute();
		
		lsIntent.setOnItemClickListener(lsIntent_itemClick);
		Log.d(TAG, "2changing config: " + getChangingConfigurations());
	}

	private OnItemClickListener lsIntent_itemClick = new OnItemClickListener() {
		@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        Intent intent = mAdapter.intentForPosition(position);
	        startActivity(intent);
	        finish();
		}
	};
	
	private final class DisplayResolveInfo {
		ResolveInfo ri;
		CharSequence displayLabel;
		Drawable displayIcon;
		CharSequence extendedInfo;
		Intent origIntent;
		List<ImageView> loadedListeners;

		DisplayResolveInfo(ResolveInfo pri, CharSequence pLabel, CharSequence pInfo, Intent pOrigIntent) {
			ri = pri;
			displayLabel = pLabel;
			extendedInfo = pInfo;
			origIntent = pOrigIntent;
		}
	}

	private final class ResolveListAdapter extends BaseAdapter {
		private final Intent mIntent;
		private final LayoutInflater mInflater;

		private List<DisplayResolveInfo> mList;

		public ResolveListAdapter(Context context, Intent intent, Intent[] initialIntents, List<ResolveInfo> rList) {
			mIntent = new Intent(intent);
			mIntent.setComponent(null);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			if (rList == null) {
				rList = mPm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			}
			int N;
			if ((rList != null) && ((N = rList.size()) > 0)) {
				// Only display the first matches that are either of equal
				// priority or have asked to be default options.
				ResolveInfo r0 = rList.get(0);
				for (int i = 1; i < N; i++) {
					ResolveInfo ri = rList.get(i);
					Log.v("ResolveListActivity", r0.activityInfo.name + "=" + r0.priority + "/" + r0.isDefault + " vs " + ri.activityInfo.name + "=" + ri.priority + "/" + ri.isDefault);
					if (r0.priority != ri.priority || r0.isDefault != ri.isDefault) {
						while (i < N) {
							rList.remove(i);
							N--;
						}
					}
				}
				if (N > 1) {
					ResolveInfo.DisplayNameComparator rComparator = new ResolveInfo.DisplayNameComparator(mPm);
					Collections.sort(rList, rComparator);
				}

				mList = new ArrayList<DisplayResolveInfo>();

				// First put the initial items at the top.
				if (initialIntents != null) {
					for (int i = 0; i < initialIntents.length; i++) {
						Intent ii = initialIntents[i];
						if (ii == null) {
							continue;
						}
						ActivityInfo ai = ii.resolveActivityInfo(getPackageManager(), 0);
						if (ai == null) {
							Log.w("ResolverActivity", "No activity found for " + ii);
							continue;
						}
						ResolveInfo ri = new ResolveInfo();
						ri.activityInfo = ai;
						if (ii instanceof LabeledIntent) {
							LabeledIntent li = (LabeledIntent) ii;
							ri.resolvePackageName = li.getSourcePackage();
							ri.labelRes = li.getLabelResource();
							ri.nonLocalizedLabel = li.getNonLocalizedLabel();
							ri.icon = li.getIconResource();
						}
						mList.add(new DisplayResolveInfo(ri, ri.loadLabel(getPackageManager()), null, ii));
					}
				}

				// Check for applications with same name and use application name or
				// package name if necessary
				r0 = rList.get(0);
				int start = 0;
				CharSequence r0Label = r0.loadLabel(mPm);
				for (int i = 1; i < N; i++) {
					if (r0Label == null) {
						r0Label = r0.activityInfo.packageName;
					}
					ResolveInfo ri = rList.get(i);
					CharSequence riLabel = ri.loadLabel(mPm);
					if (riLabel == null) {
						riLabel = ri.activityInfo.packageName;
					}
					if (riLabel.equals(r0Label)) {
						continue;
					}
					processGroup(rList, start, (i - 1), r0, r0Label);
					r0 = ri;
					r0Label = riLabel;
					start = i;
				}
				// Process last group
				processGroup(rList, start, (N - 1), r0, r0Label);
			}
		}

		private void processGroup(List<ResolveInfo> rList, int start, int end, ResolveInfo ro, CharSequence roLabel) {
			// Process labels from start to i
			int num = end - start + 1;
			if (num == 1) {
				// No duplicate labels. Use label for entry at start
				mList.add(new DisplayResolveInfo(ro, roLabel, null, null));
			} else {
				boolean usePkg = false;
				CharSequence startApp = ro.activityInfo.applicationInfo.loadLabel(mPm);
				if (startApp == null) {
					usePkg = true;
				}
				if (!usePkg) {
					// Use HashSet to track duplicates
					HashSet<CharSequence> duplicates = new HashSet<CharSequence>();
					duplicates.add(startApp);
					for (int j = start + 1; j <= end; j++) {
						ResolveInfo jRi = rList.get(j);
						CharSequence jApp = jRi.activityInfo.applicationInfo.loadLabel(mPm);
						if ((jApp == null) || (duplicates.contains(jApp))) {
							usePkg = true;
							break;
						} else {
							duplicates.add(jApp);
						}
					}
					// Clear HashSet for later use
					duplicates.clear();
				}
				for (int k = start; k <= end; k++) {
					ResolveInfo add = rList.get(k);
					if (usePkg) {
						// Use application name for all entries from start to end-1
						mList.add(new DisplayResolveInfo(add, roLabel, add.activityInfo.packageName, null));
					} else {
						// Use package name for all entries from start to end-1
						mList.add(new DisplayResolveInfo(add, roLabel, add.activityInfo.applicationInfo.loadLabel(mPm), null));
					}
				}
			}
		}

		public Intent intentForPosition(int position) {
			if (mList == null) {
				return null;
			}

			DisplayResolveInfo dri = mList.get(position);

			Intent intent = new Intent(dri.origIntent != null ? dri.origIntent : mIntent);
			intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
			ActivityInfo ai = dri.ri.activityInfo;
			intent.setComponent(new ComponentName(ai.applicationInfo.packageName, ai.name));
			return intent;
		}

		@Override public int getCount() {
			return mList != null ? mList.size() : 0;
		}

		@Override public Object getItem(int position) {
			return position;
		}

		@Override public long getItemId(int position) {
			return position;
		}

		@Override public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.item_share_intent, parent, false);
			} else {
				view = convertView;
			}
			bindView(view, mList.get(position));
			return view;
		}

		private final void bindView(View view, final DisplayResolveInfo info) {
			TextView text = (TextView) view.findViewById(android.R.id.text1);
			TextView text2 = (TextView) view.findViewById(android.R.id.text2);
			final ImageView icon = (ImageView) view.findViewById(R.id.icon);
			
			text.setText(info.displayLabel);
			if (info.extendedInfo != null) {
				text2.setVisibility(View.VISIBLE);
				text2.setText(info.extendedInfo);
			} else {
				text2.setVisibility(View.GONE);
			}
			
			// patch by yuku: load icon di bekgron supaya ga bikin hang
			icon.setTag(info);
			if (info.displayIcon != null) {
				icon.setImageDrawable(info.displayIcon);
			} else {
				icon.setImageDrawable(null); // kosongin dulu
				
				if (info.loadedListeners == null) {
					info.loadedListeners = new ArrayList<ImageView>();
					info.loadedListeners.add(icon);
					
					new AsyncTask<Void, Void, Drawable>() {
						@Override protected Drawable doInBackground(Void... params) {
							Drawable res = info.ri.loadIcon(mPm);
							info.displayIcon = res; // set data only
							return res;
						}
						
						@Override protected void onPostExecute(Drawable result) {
							// set drawable if icon still refers to this info
							for (ImageView listener: info.loadedListeners) {
								if (listener.getTag() == info) {
									listener.setImageDrawable(result);
								}
							}
							// clear listeners
							info.loadedListeners = null;
						};
					}.execute();
				} else {
					// udah loading, tambah listener aja
					info.loadedListeners.add(icon);
				}
			}
		}
	}
}

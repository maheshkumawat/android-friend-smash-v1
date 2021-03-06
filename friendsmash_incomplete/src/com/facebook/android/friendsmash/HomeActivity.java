/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android.friendsmash;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.AppEventsLogger;
import com.facebook.FacebookRequestError;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

/**
 *  Entry point for the app that represents the home screen with the Play button etc. and
 *  also the login screen for the social version of the app - these screens will switch
 *  within this activity using Fragments.
 */
public class HomeActivity extends FragmentActivity {
	
	// Tag used when logging messages
    private static final String TAG = HomeActivity.class.getSimpleName();
    
    // Uri used in handleError() below
    private static final Uri M_FACEBOOK_URL = Uri.parse("http://m.facebook.com");
    
    // Fragment attributes
    private static final int FB_LOGGED_OUT_HOME = 0;
    private static final int HOME = 1;
    private static final int FRAGMENT_COUNT = HOME +1;
    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
 	
 	// Boolean recording whether the activity has been resumed so that
 	// the logic in onSessionStateChange is only executed if this is the case
 	private boolean isResumed = false;
    
 	// Constructor
 	public HomeActivity() {
 		super();
 	} 	
 	
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setContentView(R.layout.home);
		
		FragmentManager fm = getSupportFragmentManager();
        fragments[FB_LOGGED_OUT_HOME] = fm.findFragmentById(R.id.fbLoggedOutHomeFragment);
        fragments[HOME] = fm.findFragmentById(R.id.homeFragment);

        FragmentTransaction transaction = fm.beginTransaction();
        for(int i = 0; i < fragments.length; i++) {
            transaction.hide(fragments[i]);
        }
        transaction.commit();        
		
		// Restore the logged-in user's information if it has been saved and the existing data in the application
		// has been destroyed (i.e. the app hasn't been used for a while and memory on the device is low)
		// - only do this if the session is open for the social version only
 		if (FriendSmashApplication.IS_SOCIAL) {
			// loggedIn
 			if (savedInstanceState != null) {
				boolean loggedInState = savedInstanceState.getBoolean(FriendSmashApplication.getLoggedInKey(), false);
	 			((FriendSmashApplication)getApplication()).setLoggedIn(loggedInState);
	 			
		 		if ( ((FriendSmashApplication)getApplication()).isLoggedIn() &&
		 			 ( ((FriendSmashApplication)getApplication()).getFriends() == null ||
		 			   ((FriendSmashApplication)getApplication()).getCurrentFBUser() == null) ) {
	 				try {
	 					// currentFBUser
	 					String currentFBUserJSONString = savedInstanceState.getString(FriendSmashApplication.getCurrentFbUserKey());
	 					if (currentFBUserJSONString != null) {
		 					GraphUser currentFBUser = GraphObject.Factory.create(new JSONObject(currentFBUserJSONString), GraphUser.class);
		 					((FriendSmashApplication)getApplication()).setCurrentFBUser(currentFBUser);
	 					}
	 			        
	 			        // friends
	 					ArrayList<String> friendsJSONStringArrayList = savedInstanceState.getStringArrayList(FriendSmashApplication.getFriendsKey());
	 					if (friendsJSONStringArrayList != null) {
		 					ArrayList<GraphUser> friends = new ArrayList<GraphUser>();
		 					Iterator<String> friendsJSONStringArrayListIterator = friendsJSONStringArrayList.iterator();
		 					while (friendsJSONStringArrayListIterator.hasNext()) {
		 							friends.add(GraphObject.Factory.create(new JSONObject(friendsJSONStringArrayListIterator.next()), GraphUser.class));
		 					}
		 					((FriendSmashApplication)getApplication()).setFriends(friends);
	 					}
	 				} catch (JSONException e) {
	 					Log.e(FriendSmashApplication.TAG, e.toString());
	 				}
 				}
	 		}
 		}
    }
 	
 	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
 		super.onActivityResult(requestCode, resultCode, data);
    }
	
 	@Override
    protected void onResumeFragments() {
		super.onResumeFragments();
		if (!FriendSmashApplication.IS_SOCIAL) {
			showFragment(HOME, false);
		} else {
			Session session = Session.getActiveSession();
			if (session != null && session.isOpened() && ((FriendSmashApplication)getApplication()).getCurrentFBUser() != null) {
				showFragment(HOME, false);
			} else {
				showFragment(FB_LOGGED_OUT_HOME, false);
			}
		}
    }
	
	@Override
    public void onResume() {
        super.onResume();
        isResumed = true;
        
        // Hide the notification bar
 		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
 		
 		// Measure mobile app install ads
 		// Ref: https://developers.facebook.com/docs/tutorials/mobile-app-ads/
 		AppEventsLogger.activateApp(this, ((FriendSmashApplication)getApplication()).getString(R.string.app_id));
    }

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        
  		// Save the logged-in state
  		outState.putBoolean(FriendSmashApplication.getLoggedInKey(), ((FriendSmashApplication)getApplication()).isLoggedIn());
  		
        // Save the currentFBUser
        if (((FriendSmashApplication)getApplication()).getCurrentFBUser() != null) {
	        outState.putString(FriendSmashApplication.getCurrentFbUserKey(),
	        		((FriendSmashApplication)getApplication()).getCurrentFBUser().getInnerJSONObject().toString());
        }
        
        // Save the logged-in user's list of friends
        if (((FriendSmashApplication)getApplication()).getFriends() != null) {
	        outState.putStringArrayList(FriendSmashApplication.getFriendsKey(),
	        		((FriendSmashApplication)getApplication()).getFriendsAsArrayListOfStrings());
        }
	}
	
	@Override
    public void onDestroy() {
 		super.onDestroy();
    }
		
	public void buyBombs() {
		// update bomb and coins count (5 coins per bomb)
		FriendSmashApplication app = (FriendSmashApplication) getApplication();

		// check to see that we have enough coins.
		if (app.getCoins() - 5 < 0) {
			Toast.makeText(this, "Not enough coins.", Toast.LENGTH_LONG).show();
			return;
		}

		app.setBombs(app.getBombs()+1);
		app.setCoins(app.getCoins()-5);

		// save inventory values
		app.saveInventory();
        
        // reload inventory values in fragment
        loadInventoryFragment();
	}
	
    private void showFragment(int fragmentIndex, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (int i = 0; i < fragments.length; i++) {
            if (i == fragmentIndex) {
                transaction.show(fragments[i]);
            } else {
                transaction.hide(fragments[i]);
            }
        }
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
        
        // Do other changes depending on the fragment that is now showing
        if (FriendSmashApplication.IS_SOCIAL) {
        	switch (fragmentIndex) {
        		case FB_LOGGED_OUT_HOME:
        			// Hide the progressContainer in FBLoggedOutHomeFragment 
        			if (fragments[FB_LOGGED_OUT_HOME] != null && ((FBLoggedOutHomeFragment)fragments[FB_LOGGED_OUT_HOME]) != null) {
        				((FBLoggedOutHomeFragment)fragments[FB_LOGGED_OUT_HOME]).progressContainer.setVisibility(View.INVISIBLE);
        			}
        			// Set the loggedIn attribute
        			((FriendSmashApplication)getApplication()).setLoggedIn(false);
        			break;
        		case HOME:
        			// Update the youScoredTextView in HomeFragment
        			if (fragments[HOME] != null) {
        				((HomeFragment)fragments[HOME]).updateYouScoredTextView();
        				((HomeFragment)fragments[HOME]).updateButtonVisibility();
        			}
        			// Set the loggedIn attribute
        			((FriendSmashApplication)getApplication()).setLoggedIn(true);
        			break;
        	}
        }
    }
	
	/* Facebook Integration Only ... */

	// Call back on HomeActivity when the session state changes to update the view accordingly
	private void updateView() {
		if (isResumed) {
			Session session = Session.getActiveSession();
			if (session.isOpened() && !((FriendSmashApplication)getApplication()).isLoggedIn() && fragments[HOME] != null) {
				// Not logged in, but should be, so fetch the user information and log in (load the HomeFragment)
				fetchUserInformationAndLogin();
	        } else if (session.isClosed() && ((FriendSmashApplication)getApplication()).isLoggedIn() && fragments[FB_LOGGED_OUT_HOME] != null) {
				// Logged in, but shouldn't be, so load the FBLoggedOutHomeFragment
	        	showFragment(FB_LOGGED_OUT_HOME, false);
	        }
			
			// Note that error checking for failed logins is done as within an ErrorListener attached to the
			// LoginButton within FBLoggedOutHomeFragment
		}
	}
	
	// Fetch user information and login (i.e switch to the personalized HomeFragment)
	private void fetchUserInformationAndLogin() {
		loadPersonalizedFragment();
	}
	
	// Loads the inventory portion of the HomeFragment. 
    private void loadInventoryFragment() {
    	Log.d(TAG, "Loading inventory fragment");
    	if (isResumed) {
			((HomeFragment)fragments[HOME]).loadInventory();
		} else {
			showError(getString(R.string.error_switching_screens), true);
		}
    }

	// Switches to the personalized HomeFragment as the user has just logged in
	private void loadPersonalizedFragment() {
		if (isResumed) {
			// Personalize the HomeFragment
			((HomeFragment)fragments[HOME]).personalizeHomeFragment();
			
			// Load the HomeFragment personalized
			showFragment(HOME, false);
		} else {
			showError(getString(R.string.error_switching_screens), true);
		}
	}
	
    void handleError(FacebookRequestError error, boolean logout) {
        DialogInterface.OnClickListener listener = null;
        String dialogBody = null;

        if (error == null) {
            dialogBody = getString(R.string.error_dialog_default_text);
        } else {
            switch (error.getCategory()) {
                case AUTHENTICATION_RETRY:
                    // tell the user what happened by getting the message id, and
                    // retry the operation later
                    String userAction = (error.shouldNotifyUser()) ? "" :
                            getString(error.getUserActionMessageId());
                    dialogBody = getString(R.string.error_authentication_retry, userAction);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, M_FACEBOOK_URL);
                            startActivity(intent);
                        }
                    };
                    break;

                case AUTHENTICATION_REOPEN_SESSION:
                    // close the session and reopen it.
                    dialogBody = getString(R.string.error_authentication_reopen);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Session session = Session.getActiveSession();
                            if (session != null && !session.isClosed()) {
                                session.closeAndClearTokenInformation();
                            }
                        }
                    };
                    break;

                case PERMISSION:
                    // request the publish permission
                    dialogBody = getString(R.string.error_permission);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        	if (fragments[HOME] != null) {
                        		((HomeFragment) fragments[HOME]).setPendingPost(true);
                        		((HomeFragment) fragments[HOME]).requestPublishPermissions(Session.getActiveSession());
                        	}
                        }
                    };
                    break;

                case SERVER:
                case THROTTLING:
                    // this is usually temporary, don't clear the fields, and
                    // ask the user to try again
                    dialogBody = getString(R.string.error_server);
                    break;

                case BAD_REQUEST:
                    // this is likely a coding error, ask the user to file a bug
                    dialogBody = getString(R.string.error_bad_request, error.getErrorMessage());
                    break;

                case CLIENT:
                	// this is likely an IO error, so tell the user they have a network issue
                	dialogBody = getString(R.string.network_error);
                    break;
                    
                case OTHER:
                default:
                    // an unknown issue occurred, this could be a code error, or
                    // a server side issue, log the issue, and either ask the
                    // user to retry, or file a bug
                    dialogBody = getString(R.string.error_unknown, error.getErrorMessage());
                    break;
            }
        }

        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.error_dialog_button_text, listener)
                .setTitle(R.string.error_dialog_title)
                .setMessage(dialogBody)
                .show();
        
        if (logout) {
        	logout();
        }
    }
	
	// Show user error message as a toast
	void showError(String error, boolean logout) {
		Toast.makeText(this, error, Toast.LENGTH_LONG).show();
		if (logout) {
			logout();
		}
	}
	
    private void logout() {
    	// Close the session, which will cause a callback to show the logout screen
		Session.getActiveSession().closeAndClearTokenInformation();
		
		// Clear any permissions associated with the LoginButton
		LoginButton loginButton = (LoginButton) findViewById(R.id.loginButton);
		if (loginButton != null) {
			loginButton.clearPermissions();
		}
    }
	
}

package com.example.android.inventory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.ItemContract;
import com.example.android.inventory.data.ItemContract.ItemEntry;
import com.squareup.picasso.Picasso;

/**
 * Allows user to create a new item or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the image request
     */
    public static final int PICK_IMAGE_REQUEST = 10;
    private static final String LOG_TAG = EditorActivity.class.getSimpleName();
    /**
     * Identifier for the item data loader
     */
    private static final int EXISTING_ITEM_LOADER = 0;
    /**
     * Content URI for the existing item (null if it's a new item)
     */
    private Uri mCurrentItemUri;

    /**
     * variable to store if something on the edit screen has changed
     */
    private boolean mItemHasChanged = false;

    /**
     * Create an OnTouchListener to monitor inputs
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    /**
     * EditText field to enter the item's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the item's supplier
     */
    private EditText mSupplierEditText;

    /**
     * EditText field to enter the item's price
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the item's quantity
     */
    private EditText mQuantityEditText;

    /**
     * Order button to order more items from the supplier
     */
    private Button mOrderButton;

    /**
     * EditText field to enter the item's quantity
     */
    private TextView mQuantityPlusButton;

    /**
     * EditText field to enter the item's quantity
     */
    private TextView mQuantityMinusButton;

    /**
     * ImageView field for the item image
     */
    private ImageView mItemImageView;

    /**
     * EditText field to enter the item's quantity
     */
    private TextView mEmptyImageTextView;

    /**
     * Name of the item
     */
    private String mItemName;

    /**
     * EditText field to enter the item's quantity
     */
    private String mCurrentImageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new item or editing an existing one.
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // If the intent DOES NOT contain a item content URI, then we know that we are
        // creating a new item.
        if (mCurrentItemUri == null) {
            // This is a new item, so change the app bar to say "Add a Item"
            setTitle(getString(R.string.editor_activity_title_new_item));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a item that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing item, so change app bar to say "Edit Item"
            setTitle(getString(R.string.editor_activity_title_edit_item));

            // Initialize a loader to read the item data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mSupplierEditText = (EditText) findViewById(R.id.edit_supplier);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mOrderButton = (Button) findViewById(R.id.order_button);
        mQuantityMinusButton = (TextView) findViewById(R.id.minus_quantity);
        mQuantityPlusButton = (TextView) findViewById(R.id.plus_quantity);
        mItemImageView = (ImageView) findViewById(R.id.image_view);
        mEmptyImageTextView = (TextView) findViewById(R.id.empty_image);

        // Set listeners on the input fields
        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mItemImageView.setOnTouchListener(mTouchListener);

        // Set click listener for order button
        mOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mItemName = String.valueOf(mNameEditText.getText());

                // Send email for an order
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this

                // Can be used if email of supplier is saved in the database
                // intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email_address)});

                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject) + " " + mItemName);
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_text));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        // Set Click Listener for add/subract quantity button clicks
        mQuantityMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Read out current value and only decrease if not null or 0
                String previousValueString = mQuantityEditText.getText().toString();
                int previousValue;
                if (!previousValueString.isEmpty() && !previousValueString.equals("0")) {

                    try {
                        previousValue = Integer.parseInt(previousValueString);
                        mQuantityEditText.setText(String.valueOf(previousValue - 1));
                    } catch (NumberFormatException e) {
                        Toast.makeText(EditorActivity.this, R.string.message_wrong_number_format, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        });

        mQuantityPlusButton.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {

                // Read out current value and if null, set to zero. In any case, add 1 to the value
                String previousValueString = mQuantityEditText.getText().toString();
                int previousValue;


                if (previousValueString.isEmpty()) {
                    previousValue = 0;
                } else {
                    try {
                        previousValue = Integer.parseInt(previousValueString);
                    } catch (NumberFormatException e) {
                        Toast.makeText(EditorActivity.this, R.string.message_wrong_number_format, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }


                mQuantityEditText.setText(String.valueOf(previousValue + 1));


            }
        });


        // Set Click Listener on ImageView so the user can select an image
        mItemImageView.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {

                // Call method which starts the image selection
                openImageSelector();
            }
        });
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }


    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    /**
     * Get user input from editor and save item into database.
     */
    private void saveItem() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();


        if (TextUtils.isEmpty(nameString) ||
                TextUtils.isEmpty(supplierString) ||
                TextUtils.isEmpty(priceString) ||
                TextUtils.isEmpty(quantityString) ||
                mCurrentImageUri == null
                ) {

            // Since fields may not be empty we return early and show a toast message
            Toast.makeText(this, R.string.message_empty_input, Toast.LENGTH_SHORT).show();
            return;
        }

        // If the price is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        double price;
        int quantity;

        // Try if the input format is correct, if not show a toast message and return early
        try {
            price = Double.parseDouble(priceString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.message_wrong_input_format, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            quantity = Integer.parseInt(quantityString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.message_wrong_input_format, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and item attributes from the editor are the values.
        ContentValues values = new ContentValues();

        // Insert values into ContentValues object
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER, supplierString);
        values.put(ItemEntry.COLUMN_ITEM_IMAGE_URI, mCurrentImageUri);
        values.put(ItemEntry.COLUMN_ITEM_PRICE, price);
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity);

        // Determine if this is a new or existing item by checking if mCurrentItemUri is null or not
        if (mCurrentItemUri == null) {
            // This is a NEW item, so insert a new item into the provider,
            // returning the content URI for the new item.
            Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING item, so update the item with content URI: mCurrentItemUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentItemUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Exit activity
        finish();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {

        // Only perform the delete if this is an existing item.
        if (mCurrentItemUri != null) {
            // Call the ContentResolver to delete the item at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentItemUri
            // content URI already identifies the item that we want.
            int rowsDeleted = getContentResolver().delete(
                    mCurrentItemUri,
                    null,
                    null
            );


            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Exit activity
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }


    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save item to database
                saveItem();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Call delete dialog
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Code sample from https://github.com/crlsndrsjmnz/MyShareImageExample
    // Check SDK Version and set according intent action
    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Method that is called after image picker has been called and a result is given
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                Uri resultUri = resultData.getData();
                mCurrentImageUri = resultUri.toString();
                Log.i(LOG_TAG, "Uri: " + mCurrentImageUri);

                // Load the image into the ImageView
                if (mCurrentImageUri != null) {
                    Picasso.with(this)
                            .load(mCurrentImageUri)
                            .fit().centerCrop()
                            .into(mItemImageView);

                    // Hide the text to select an image
                    mEmptyImageTextView.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the item table
        String[] projection = {
                ItemContract.ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_SUPPLIER,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_IMAGE_URI};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Hide the text to select an image
        mEmptyImageTextView.setVisibility(View.GONE);

        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int supplierColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int imageUriIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE_URI);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            mCurrentImageUri = cursor.getString(imageUriIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mSupplierEditText.setText(supplier);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Double.toString(price));

            // TODO: Check not working yet

/*
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor fileCursor = this.getContentResolver().query(Uri.parse(mCurrentImageUri), filePathColumn, null, null, null);
            fileCursor.moveToFirst();
            int columnIndex = fileCursor.getColumnIndex(filePathColumn[0]);
            String filePath = fileCursor.getString(columnIndex);
            fileCursor.close();

            // Check if there is an image and load it into the ImageView
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
*/
            // Load image into image view. If the uri points to an empty path, set a placeholder imgae
            if (mCurrentImageUri != null) {
                Picasso.with(this)
                        .load(mCurrentImageUri)
                        .fit().centerCrop()
                        .placeholder(R.drawable.ic_empty_shelter)
                        .error(R.drawable.ic_empty_shelter)
                        .into(mItemImageView);
            } else {
                mEmptyImageTextView.setText("Image not found. Please select a new image");
                mEmptyImageTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mSupplierEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mItemImageView.setImageDrawable(null);
    }
}
package org.shagi.filepickersample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.shagi.filepicker.*
import timber.log.Timber

class MainActivity : AppCompatActivity(), FilePicker.OnLoadingListener {
    private var keys: MutableList<Long> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("DebugTag, onCreate $this")

        setContentView(R.layout.activity_main)
        val listener: View.OnClickListener

        val filePickerDialog = FilePickerDialog.newInstance().apply {
            multipleSelect = true
            addCustomAction(
                    CustomActionItem(R.drawable.file_picker_ic_folder,
                            R.string.fpd_load_from_medical_note_documents,
                            View.OnClickListener {
                                Toast.makeText(context, "1 $context", Toast.LENGTH_SHORT).show()
                            })
            )
            addCustomAction(
                    CustomActionItem(R.drawable.file_picker_ic_folder,
                            R.string.fpd_load_from_medical_note_documents,
                            View.OnClickListener {
                                Toast.makeText(context, "2  $context", Toast.LENGTH_SHORT).show()
                            })
            )
        }

        if (savedInstanceState == null) {
            listener = View.OnClickListener {
                FilePickerFragment.getFragment(supportFragmentManager, false).apply {
                    setOnLoadingListener(this@MainActivity)
                    use(filePickerDialog)
                }.show()
            }
        } else {
            keys = savedInstanceState.getLongArray(ARG_KEYS).toMutableList()
            val fragment = FilePickerFragment.getFragment(supportFragmentManager).apply {
                setOnLoadingListener(this@MainActivity)
                use(filePickerDialog)
            }
            listener = View.OnClickListener { fragment.show() }
        }

        image_1.setOnClickListener(listener)
        image_2.setOnClickListener(listener)
        image_3.setOnClickListener(listener)
        image_4.setOnClickListener(listener)

        Timber.d("listener ${image_1.hasOnClickListeners()}")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(ARG_KEYS, keys.toLongArray())
    }

    override fun onLoadingStart(key: Long) {
        progress.visibility = View.VISIBLE
        keys.add(key)
        Timber.d("DebugTag, main onLoadingStart $this with key $key")
    }

    override fun onLoadingSuccess(key: Long, file: ExtFile) {
        progress.visibility = View.GONE

        Timber.d("DebugTag, onLoadingSuccess $key" +
                "\n ${file.file}" +
                "\n ${file.baseUri}" +
                "\n ${file.name}" +
                "\n ${file.extension}" +
                "\n ${file.mimeType}")

        val imageView: ImageView = when (keys.size) {
            0 -> image_1
            1 -> image_2
            2 -> image_3
            3 -> image_4
            else -> image_1
        }

        keys.remove(key)

        Picasso.get()
                .load(file.file)
                .into(imageView)

        Timber.d("DebugTag, main onLoadingSuccess $this with key $key")
    }

    override fun onLoadingFailure(key: Long, throwable: Throwable) {
        progress.visibility = View.GONE
        Timber.d("DebugTag, main onLoadingFailure $this with key $key")
        Timber.e(throwable)
    }

    companion object {
        const val ARG_KEYS = "arg_keys"
    }
}

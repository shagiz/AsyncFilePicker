package org.shagi.filepickersample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_sample.*
import org.shagi.filepicker.*

class SampleActivity : AppCompatActivity() {
    private var useCache = false
    private var multipleSelect = false
    private var cameraDisabled = false
    private var galleryDisabled = false
    private var filesDisabled = false
    private var customActionDisabled = false
    private var useCustomLayout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val pickerFragment = FilePickerFragment.getFragment(supportFragmentManager)

        sample_file_container_1.setOnClickListener {
            pickerFragment.use(initFilePickerDialog())
            pickerFragment.setOnLoadingListener(object : FilePicker.OnLoadingListener {
                override fun onLoadingStart(key: Long) {
                    sample_file_container_1_iv.setImageResource(R.color.colorAccent)
                    sample_file_container_1_info.text = "loading"
                }

                override fun onLoadingSuccess(key: Long, file: ExtFile) {
                    sample_file_container_1_info.text = file.toString()

                    Picasso.get()
                            .load(file.file)
                            .memoryPolicy(MemoryPolicy.NO_CACHE)
                            .into(sample_file_container_1_iv)
                }

                override fun onLoadingFailure(key: Long, throwable: Throwable) {
                    sample_file_container_1_info.text = throwable.message
                }

            })
            pickerFragment.show()
        }

        sample_file_container_2.setOnClickListener {
            pickerFragment.use(initFilePickerDialog())
            pickerFragment.setOnLoadingListener(object : FilePicker.OnLoadingListener {
                override fun onLoadingStart(key: Long) {
                    sample_file_container_2_iv.setImageResource(R.color.colorAccent)
                    sample_file_container_2_info.text = "loading"
                }

                override fun onLoadingSuccess(key: Long, file: ExtFile) {
                    sample_file_container_2_info.text = file.toString()

                    Picasso.get()
                            .load(file.file)
                            .memoryPolicy(MemoryPolicy.NO_CACHE)
                            .into(sample_file_container_2_iv)
                }

                override fun onLoadingFailure(key: Long, throwable: Throwable) {
                    sample_file_container_2_info.text = throwable.message
                }

            })
            pickerFragment.show()
        }

        sample_file_container_3.setOnClickListener {
            pickerFragment.use(initFilePickerDialog())
            pickerFragment.setOnLoadingListener(object : FilePicker.OnLoadingListener {
                override fun onLoadingStart(key: Long) {
                    sample_file_container_3_iv.setImageResource(R.color.colorAccent)
                    sample_file_container_3_info.text = "loading"
                }

                override fun onLoadingSuccess(key: Long, file: ExtFile) {
                    sample_file_container_3_info.text = file.toString()

                    Picasso.get()
                            .load(file.file)
                            .into(sample_file_container_3_iv)
                }

                override fun onLoadingFailure(key: Long, throwable: Throwable) {
                    sample_file_container_3_info.text = throwable.message
                }

            })
            pickerFragment.show()
        }

        sample_file_container_4.setOnClickListener {
            pickerFragment.use(initFilePickerDialog())
            pickerFragment.setOnLoadingListener(object : FilePicker.OnLoadingListener {
                override fun onLoadingStart(key: Long) {
                    sample_file_container_4_iv.setImageResource(R.color.colorAccent)
                    sample_file_container_4_info.text = "loading"
                }

                override fun onLoadingSuccess(key: Long, file: ExtFile) {
                    sample_file_container_4_info.text = file.toString()

                    Picasso.get()
                            .load(file.file)
                            .memoryPolicy(MemoryPolicy.NO_CACHE)
                            .into(sample_file_container_4_iv)
                }

                override fun onLoadingFailure(key: Long, throwable: Throwable) {
                    sample_file_container_4_info.text = throwable.message
                }

            })
            pickerFragment.show()
        }

        sample_miltiple_select.setOnClickListener {
            multipleSelect = !multipleSelect
            sample_miltiple_select.isChecked = !sample_miltiple_select.isChecked
        }

        sample_disable_camera.setOnClickListener {
            cameraDisabled = !cameraDisabled
            sample_disable_camera.isChecked = !sample_disable_camera.isChecked
        }

        sample_use_custom_layout.setOnClickListener {
            useCustomLayout = !useCustomLayout
            sample_use_custom_layout.isChecked = !sample_use_custom_layout.isChecked
        }

        sample_disable_gallery.setOnClickListener {
            galleryDisabled = !galleryDisabled
            sample_disable_gallery.isChecked = !sample_disable_gallery.isChecked
        }

        sample_disable_files.setOnClickListener {
            filesDisabled = !filesDisabled
            sample_disable_files.isChecked = !sample_disable_files.isChecked
        }

        sample_disable_custom.setOnClickListener {
            customActionDisabled = !customActionDisabled
            sample_disable_custom.isChecked = !sample_disable_custom.isChecked
        }
    }

    private fun initFilePickerDialog() =
            if (useCustomLayout) { // to user Custom Picker
                CustomPicker()
            } else {
                FilePickerDialog.newInstance()
            }.apply {
                multipleSelect = this@SampleActivity.multipleSelect //multipe files\photo select
                showCamera = !cameraDisabled // to show camera picker row
                showGallery = !galleryDisabled // to show gallery picker row
                showFileSystem = !filesDisabled // to show file system picker row

                if (!customActionDisabled) { // to use custom action
                    addCustomAction(CustomActionItem(R.drawable.file_picker_ic_folder,
                            R.string.fpd_load_from_medical_note_documents,
                            View.OnClickListener {
                                Toast.makeText(context, "1 $context", Toast.LENGTH_SHORT).show()
                            })
                    )
                }
            }
}

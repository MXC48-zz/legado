package io.legado.app.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogReadAloudBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.model.ReadAloud
import io.legado.app.model.ReadBook
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.observeEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.viewbindingdelegate.viewBinding


class ReadAloudDialog : BaseDialogFragment() {
    private var callBack: CallBack? = null
    private val binding by viewBinding(DialogReadAloudBinding::bind)

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.setBackgroundDrawableResource(R.color.background)
            it.decorView.setPadding(0, 0, 0, 0)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            it.attributes = attr
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as ReadBookActivity).bottomDialog++
        callBack = activity as? CallBack
        return inflater.inflate(R.layout.dialog_read_aloud, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        val textColor = requireContext().getPrimaryTextColor(isLight)
        binding.run {
            rootView.setBackgroundColor(bg)
            tvPre.setTextColor(textColor)
            tvNext.setTextColor(textColor)
            ivPlayPrev.setColorFilter(textColor)
            ivPlayPause.setColorFilter(textColor)
            ivPlayNext.setColorFilter(textColor)
            ivStop.setColorFilter(textColor)
            ivTimer.setColorFilter(textColor)
            tvTimer.setTextColor(textColor)
            tvTtsSpeed.setTextColor(textColor)
            ivCatalog.setColorFilter(textColor)
            tvCatalog.setTextColor(textColor)
            ivMainMenu.setColorFilter(textColor)
            tvMainMenu.setTextColor(textColor)
            ivToBackstage.setColorFilter(textColor)
            tvToBackstage.setTextColor(textColor)
            ivSetting.setColorFilter(textColor)
            tvSetting.setTextColor(textColor)
            cbTtsFollowSys.setTextColor(textColor)
        }
        initData()
        initEvent()
    }

    private fun initData() = binding.run {
        upPlayState()
        upTimerText(BaseReadAloudService.timeMinute)
        seekTimer.progress = BaseReadAloudService.timeMinute
        cbTtsFollowSys.isChecked = requireContext().getPrefBoolean("ttsFollowSys", true)
        seekTtsSpeechRate.isEnabled = !cbTtsFollowSys.isChecked
        upSeekTimer()
    }

    private fun initEvent() = binding.run {
        llMainMenu.setOnClickListener {
            callBack?.showMenuBar()
            dismissAllowingStateLoss()
        }
        llSetting.setOnClickListener {
            ReadAloudConfigDialog().show(childFragmentManager, "readAloudConfigDialog")
        }
        tvPre.setOnClickListener { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }
        tvNext.setOnClickListener { ReadBook.moveToNextChapter(true) }
        ivStop.setOnClickListener {
            ReadAloud.stop(requireContext())
            dismissAllowingStateLoss()
        }
        ivPlayPause.setOnClickListener { callBack?.onClickReadAloud() }
        ivPlayPrev.setOnClickListener { ReadAloud.prevParagraph(requireContext()) }
        ivPlayNext.setOnClickListener { ReadAloud.nextParagraph(requireContext()) }
        llCatalog.setOnClickListener { callBack?.openChapterList() }
        llToBackstage.setOnClickListener { callBack?.finish() }
        cbTtsFollowSys.setOnCheckedChangeListener { _, isChecked ->
            requireContext().putPrefBoolean("ttsFollowSys", isChecked)
            seekTtsSpeechRate.isEnabled = !isChecked
            upTtsSpeechRate()
        }
        seekTtsSpeechRate.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                AppConfig.ttsSpeechRate = seekBar.progress
                upTtsSpeechRate()
            }
        })
        seekTimer.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                upTimerText(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadAloud.setTimer(requireContext(), seekTimer.progress)
            }
        })
    }

    private fun upPlayState() {
        if (!BaseReadAloudService.pause) {
            binding.ivPlayPause.setImageResource(R.drawable.ic_pause_24dp)
        } else {
            binding.ivPlayPause.setImageResource(R.drawable.ic_play_24dp)
        }
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        val textColor = requireContext().getPrimaryTextColor(isLight)
        binding.ivPlayPause.setColorFilter(textColor)
    }

    private fun upSeekTimer() {
        binding.seekTimer.post {
            if (BaseReadAloudService.timeMinute > 0) {
                binding.seekTimer.progress = BaseReadAloudService.timeMinute
            } else {
                binding.seekTimer.progress = 0
            }
        }
    }

    private fun upTimerText(timeMinute: Int) {
        binding.tvTimer.text = requireContext().getString(R.string.timer_m, timeMinute)
    }

    private fun upTtsSpeechRate() {
        ReadAloud.upTtsSpeechRate(requireContext())
        if (!BaseReadAloudService.pause) {
            ReadAloud.pause(requireContext())
            ReadAloud.resume(requireContext())
        }
    }

    override fun observeLiveBus() {
        observeEvent<Int>(EventBus.ALOUD_STATE) { upPlayState() }
        observeEvent<Int>(EventBus.TTS_DS) { binding.seekTimer.progress = it }
    }

    interface CallBack {
        fun showMenuBar()
        fun openChapterList()
        fun onClickReadAloud()
        fun finish()
    }
}
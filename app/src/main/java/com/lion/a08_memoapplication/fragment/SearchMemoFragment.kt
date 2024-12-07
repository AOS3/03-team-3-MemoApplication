package com.lion.a08_memoapplication.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lion.a08_memoapplication.MainActivity
import com.lion.a08_memoapplication.R
import com.lion.a08_memoapplication.databinding.FragmentSearchMemoBinding
import com.lion.a08_memoapplication.databinding.RowMemoBinding
import com.lion.a08_memoapplication.model.MemoModel
import com.lion.a08_memoapplication.repository.MemoRepository
import com.lion.a08_memoapplication.util.FragmentName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchMemoFragment : Fragment() {

    lateinit var fragmentSearchMemoBinding: FragmentSearchMemoBinding
    lateinit var mainActivity: MainActivity
    private var memoList = mutableListOf<MemoModel>()
    private var filteredList = mutableListOf<MemoModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentSearchMemoBinding = FragmentSearchMemoBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity

        settingToolbar()
        setupRecyclerView()
        setupSearchInput()

        return fragmentSearchMemoBinding.root
    }

    // 툴바를 구성하는 메서드
    fun settingToolbar() {
        fragmentSearchMemoBinding.apply {
            toolbarSearch.title = "검색하기"

            toolbarSearch.setNavigationIcon(R.drawable.menu_24px)

            toolbarSearch.setNavigationOnClickListener {
                mainActivity.activityMainBinding.drawerLayoutMain.open()
            }
        }
    }


    private fun setupRecyclerView() {
        fragmentSearchMemoBinding.recyclerViewSearchMemo.apply {
            adapter = RecyclerSearchMemoAdapter()
            layoutManager = LinearLayoutManager(mainActivity)
        }

        loadAllMemos()
    }

    private fun setupSearchInput() {
        fragmentSearchMemoBinding.editTextSearchMemo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 텍스트 입력이 변경될 때마다 필터링 함수 호출
                filterMemos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fragmentSearchMemoBinding.editTextSearchMemo.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // 엔터 키 눌렀을 때 검색 실행
                val query = v.text.toString()
                filterMemos(query)
                true
            } else {
                false
            }
        }

        // 라디오버튼 선택에 따른 검색 필터 변경
        fragmentSearchMemoBinding.radioGroupSearchType.setOnCheckedChangeListener { _, checkedId ->
            val query = fragmentSearchMemoBinding.editTextSearchMemo.text.toString()
            when (checkedId) {
                R.id.radioButtonTitle -> searchMemoByTitle(query)
                R.id.radioButtonContent -> searchMemoByContent(query)
                R.id.radioButtonTitleAndContent -> searchMemoByTitleOrContent(query)
            }
        }
    }

    private fun loadAllMemos() {
        CoroutineScope(Dispatchers.Main).launch {
            val work = async(Dispatchers.IO) {
                MemoRepository.selectMemoDataAll(mainActivity) // 기존 함수명 유지
            }
            memoList = work.await()
            filteredList = memoList.toMutableList()
            fragmentSearchMemoBinding.recyclerViewSearchMemo.adapter?.notifyDataSetChanged()
        }
    }

    // 제목만으로 메모 검색
    private fun searchMemoByTitle(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val work = async(Dispatchers.IO) {
                MemoRepository.searchMemoByTitle(mainActivity, query)
            }
            filteredList = work.await()
            fragmentSearchMemoBinding.recyclerViewSearchMemo.adapter?.notifyDataSetChanged()
        }
    }

    // 내용만으로 메모 검색
    private fun searchMemoByContent(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val work = async(Dispatchers.IO) {
                MemoRepository.searchMemoByContent(mainActivity, query)
            }
            filteredList = work.await()
            fragmentSearchMemoBinding.recyclerViewSearchMemo.adapter?.notifyDataSetChanged()
        }
    }

    // 제목 또는 내용에 포함된 메모 검색
    private fun searchMemoByTitleOrContent(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val work = async(Dispatchers.IO) {
                MemoRepository.searchMemoByTitleOrContent(mainActivity, query)
            }
            filteredList = work.await()
            fragmentSearchMemoBinding.recyclerViewSearchMemo.adapter?.notifyDataSetChanged()
        }
    }

    // 제목 또는 내용에 포함된 메모 필터링
    private fun filterMemos(query: String) {
        if (query.isBlank()) {
            filteredList = memoList.toMutableList() // 검색어가 없으면 전체 리스트를 표시
        } else {
            filteredList = memoList.filter {
                // 제목 또는 내용 중 하나라도 검색어가 포함된 메모를 찾음 (부분 일치)
                (it.memoTitle.contains(query, ignoreCase = true) ||
                        it.memoText.contains(query, ignoreCase = true)) && !it.memoIsSecret
            }.toMutableList()
        }
        fragmentSearchMemoBinding.recyclerViewSearchMemo.adapter?.notifyDataSetChanged()
    }

    inner class RecyclerSearchMemoAdapter : RecyclerView.Adapter<RecyclerSearchMemoAdapter.ViewHolder>() {

        inner class ViewHolder(val rowMemoBinding: RowMemoBinding) : RecyclerView.ViewHolder(rowMemoBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val rowMemoBinding = RowMemoBinding.inflate(layoutInflater, parent, false)
            val viewHolder = ViewHolder(rowMemoBinding)

            rowMemoBinding.root.setOnClickListener {
                val memo = filteredList[viewHolder.adapterPosition]
                val dataBundle = Bundle().apply { putInt("memoIdx", memo.memoIdx) }
                mainActivity.replaceFragment(FragmentName.READ_MEMO_FRAGMENT, true, true, dataBundle)
            }

            return viewHolder
        }

        override fun getItemCount(): Int = filteredList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val memo = filteredList[position]
            holder.rowMemoBinding.textViewRowTitle.text = memo.memoTitle
        }
    }
}

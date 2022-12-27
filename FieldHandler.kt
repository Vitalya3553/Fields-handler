class FieldHandler(private vararg val fieldProperties: FieldProperties, private val timer: Long? = null) {

    private lateinit var condition: () -> Boolean
    private var _isSleeping = MutableStateFlow(false)
    private var sleepingJob : Job? = null

    data class FieldProperties(
        val editText: TextInputEditText,
        val inputLayout: TextInputLayout,
        val errorText: String,
        val rule: (() -> Boolean)? = null,
        val execute: (() -> Unit)? = null,
    ){

        override fun equals(other: Any?): Boolean {
            return if (other is FieldProperties) {
                inputLayout == other.inputLayout || editText == other.editText
            } else {
                false
            }
        }

        override fun hashCode(): Int {

            return inputLayout.hashCode() + editText.hashCode()
        }
    }
    class UsingTheSameObjectsError : Error("You are using the same TextInputLayout or TextInputEditText objects")



    init {

        checkDuplicate()
        setErrorDrawables(0)
        setListeners()
    }



    private fun checkDuplicate(){
        if (fieldProperties.count() != fieldProperties.distinct().count()){
            throw UsingTheSameObjectsError()
        }
    }

    private fun setListeners(){

        fieldProperties.map {

            it.inputLayout.isErrorEnabled = false

            it.editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                   setTimer{
                       Log.e("Field Handler", "TExt changed")
                       val rule = it.rule
                       condition  = (rule?:  {true})
                       Log.e("Field Handler", condition().toString())
                       if(condition() && it.inputLayout.isErrorEnabled) {
                           it.inputLayout.isErrorEnabled = false
                       }

                       it.execute?.let { it1 -> it1() }
                   }

                }
                override fun afterTextChanged(s: Editable?) {}

            })

            it.editText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus){
                    condition = it.rule ?:  {true}
                    if(!condition()){
                        it.inputLayout.isErrorEnabled = true
                        it.inputLayout.error = it.errorText
                    }
                }
            }
        }

    }

    private fun setErrorDrawables(drawable: Int){
        fieldProperties.map {
            it.inputLayout.setErrorIconDrawable(drawable)
        }
    }

    private  fun setTimer(code: () -> Unit) {
        if(timer == null){
            code()
            return
        }
        if (sleepingJob!= null && sleepingJob!!.isActive){
            sleepingJob!!.cancel()
        }
        sleepingJob = CoroutineScope(Dispatchers.Main).launch {
            if(!_isSleeping.value) _isSleeping.value = true
            delay(timer)
            _isSleeping.value = false
            code()
        }
    }
    fun isAllCorrect(showErrorIfNotShowing : Boolean): Boolean {

        if (showErrorIfNotShowing){

            var isCorrect = true

            fieldProperties.map {

                condition = (it.rule ?:  {true})
                if(!condition()){

                    isCorrect = false


                    if(!it.inputLayout.isErrorEnabled ){

                        it.inputLayout.isErrorEnabled = true
                        it.inputLayout.error = it.errorText

                    }

                }
            }
            return isCorrect

        }else{
            fieldProperties.map {
                condition = (it.rule ?:  {true})
                if(!condition()) return false

            }
            return true
        }

    }
}

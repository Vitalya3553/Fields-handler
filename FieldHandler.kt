

class FieldHandler(private vararg val invalidFieldHandler: FieldProperties, private val timer: Long?) {

    private lateinit var condition: () -> Boolean
    private var isSleeping = MutableStateFlow(false)
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

    private  fun setTimer(code: () -> Unit) {
        if(timer == null){
            return
        }
        if (sleepingJob!= null && sleepingJob!!.isActive){
            sleepingJob!!.cancel()
        }
        sleepingJob = CoroutineScope(Dispatchers.Main).launch {
            if(!isSleeping.value) isSleeping.value = true

            delay(timer)

            isSleeping.value = false

            code()
        }
    }

    private fun checkDuplicate(){
        if (invalidFieldHandler.count() != invalidFieldHandler.distinct().count()){
            throw UsingTheSameObjectsError()
        }
    }

    private fun setListeners(){

        invalidFieldHandler.map {

            it.inputLayout.isErrorEnabled = false

            it.editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                   setTimer(){
                       Log.e("Field Handler", "TExt changed")
                       condition  = (it.rule ?:  {true})
                       if(condition()) {
                           it.inputLayout.isErrorEnabled = false
                       }

                       it.execute?.let { it1 -> it1() }
                   }

                }
                override fun afterTextChanged(s: Editable?) {}

            })
            if(timer == null){
                it.editText.setOnFocusChangeListener { v, hasFocus ->
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


    }
    private fun setErrorDrawables(drawable: Int){
        invalidFieldHandler.map {
            it.inputLayout.setErrorIconDrawable(drawable)
        }
    }

    fun isAllCorrect(showErrorIfNotShowing : Boolean): Boolean {

        if (showErrorIfNotShowing){

            var isCorrect = true

            invalidFieldHandler.map {

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
            invalidFieldHandler.map {
                condition = (it.rule ?:  {true}) as () -> Boolean
                if(!condition()) return false

            }
            return true
        }

    }
}

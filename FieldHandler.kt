    data class FieldProperties(val editText : TextInputEditText,
                               val inputLayout: TextInputLayout,
                               val errorText: String,
                               val rule : ()->Boolean
    ){

        override fun equals(other: Any?): Boolean {

            return if (other is FieldProperties) {
                inputLayout == other.inputLayout || editText == other.editText
            } else {
                false
            }
        }
    }
    class UsingTheSameObjectsError : Error("You are using the same TextInputLayout or TextInputEditText objects")



    init {

        checkDuplicate()
        setErrorDrawables(0)
        setListeners()
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
                    if( it.rule()) {
                        it.inputLayout.isErrorEnabled = false
                    }
                }
                override fun afterTextChanged(s: Editable?) {}

            })

            it.editText.setOnFocusChangeListener { v, hasFocus ->
                if(!hasFocus){
                    if(!it.rule()){
                        it.inputLayout.isErrorEnabled = true
                        it.inputLayout.error = it.errorText
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


                if(!it.rule()){

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
                if(!it.rule()) return false

            }
            return true
        }

    }

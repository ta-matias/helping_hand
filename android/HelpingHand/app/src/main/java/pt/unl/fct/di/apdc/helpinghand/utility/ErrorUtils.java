package pt.unl.fct.di.apdc.helpinghand.utility;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import pt.unl.fct.di.apdc.helpinghand.data.model.ErrorModel;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import retrofit2.Converter;
import retrofit2.Response;

public class ErrorUtils {

    public static ErrorModel parseError(Response<?> response) {
        HelpingHandProvider provider = new HelpingHandProvider();

        Converter<ResponseBody, ErrorModel> converter = provider.getMRetrofit().responseBodyConverter(ErrorModel.class, new Annotation[0]);

        ErrorModel errorModel = new ErrorModel();

        try{
            errorModel = converter.convert(response.errorBody());
        }catch (IOException e){
            errorModel.type = "IOException e";
            errorModel.description = e.getMessage();
            return errorModel;
        }
        return errorModel;
    }
}

package ds.gae;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import ds.gae.entities.Quote;

public class Worker extends HttpServlet {
	private static final long serialVersionUID = -7058685883212377590L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		
		List<Quote> quotes = (List<Quote>) req.getAttribute("quoteList");

		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(TaskOptions.Builder.withPayload(new ConfirmQuotesOperation(quotes)));

		resp.setContentType("text/plain");
		resp.getWriter().println("Task is backgrounded on queue!");
	}
}

ALTER TABLE `USER_API_KEY`
  ADD CONSTRAINT `uk__user_api_key__api_key` UNIQUE (`API_KEY`);
